/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.resolve.calls.InapplicableCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.WrongNumberOfTypeArguments
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeTypeIntersector
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildPlaceholderProjection
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.isRaw
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull

sealed class TypeArgumentMapping {
    abstract operator fun get(typeParameterIndex: Int): FirTypeProjection

    object NoExplicitArguments : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): FirTypeProjection = buildPlaceholderProjection()
    }

    class Mapped(private val ordered: List<FirTypeProjection>) : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): FirTypeProjection {
            return ordered.getOrElse(typeParameterIndex) { buildPlaceholderProjection() }
        }
    }
}

internal object MapTypeArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val typeArguments = callInfo.typeArguments
        val owner = candidate.symbol.fir as FirTypeParameterRefsOwner

        val (desiredTypeParameterCount: Int, refinedTypeArguments: List<FirTypeProjection>) = refineTypeArguments(
            owner,
            candidate,
            typeArguments
        )

        if (refinedTypeArguments.isEmpty()) {
            if (owner is FirCallableDeclaration && owner.dispatchReceiverType?.isRaw() == true) {
                candidate.typeArgumentMapping = computeDefaultMappingForRawTypeMember(owner, context)
            } else {
                candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
            }
            return
        }

        // Compare number of real type arguments (it's important for correct error reporting),
        // but use `refinedTypeArguments` for `typeArgumentMapping` that could include some fake type arguments
        if (
            typeArguments.size == desiredTypeParameterCount ||
            callInfo.callKind == CallKind.DelegatingConstructorCall ||
            (owner as? FirDeclaration)?.origin is FirDeclarationOrigin.DynamicScope
        ) {
            candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(refinedTypeArguments)
        } else {
            candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(
                if (refinedTypeArguments.size > desiredTypeParameterCount) refinedTypeArguments.take(desiredTypeParameterCount)
                else refinedTypeArguments
            )
            sink.yieldDiagnostic(WrongNumberOfTypeArguments(desiredTypeParameterCount, candidate.symbol))
        }
    }

    /**
     * Before creating a final `typeArgumentsMapping` we should make sure that type arguments are correct, and create new fake arguments if needed.
     * This refining is relevant while resolving constructors of inner type aliases.
     *
     * Consider the following code fragment:
     *
     * ```kt
     * class Pair<X, Y>(val x: X, val y: Y)
     * class C<T> {
     *    inner typealias P1<X> = Pair<X, T>
     * }
     *
     * // In the following line we have expanded `P1` to `Pair`
     * // But there is only one passed type argument `String` that breaks consistency of real constructor (it has two type arguments).
     * // To resolve the problem and prevent reporting of incorrect `WRONG_NUMBER_OF_TYPE_ARGUMENTS`
     * // we're extracting missing type arguments using a substitutor of existing instance (`C<Int>()`).
     * // The problem is only relevant to inner type aliases and `FirClassSubstitutionScope`,
     * // because using an inner type alias as a constructor is only valid on an existing instance of the outer class
     * // `desiredTypeParameterCount` equals to `1` for correct error reporting (because user doesn't care about fake type arguments)
     * // and we check real number of `typeArguments` with `desiredTypeParameterCount` later.
     *
     * val test1 = C<Int>().P1<String>("", 1) // it's expanded to Pair<String, Int>("", 1); `Int` is a fake type argument
     * ```
     */
    private fun refineTypeArguments(
        owner: FirTypeParameterRefsOwner,
        candidate: Candidate,
        typeArguments: List<FirTypeProjection>,
    ): Pair<Int, List<FirTypeProjection>> {
        val desiredTypeParameterCount: Int
        val refinedTypeArguments: List<FirTypeProjection>

        val constructorOwner = owner as? FirConstructor
        if (constructorOwner != null &&
            //!constructorOwner.isInner && // TODO:
            constructorOwner.origin.let { it is FirDeclarationOrigin.Synthetic.TypeAliasConstructor && it.isInner }
            && (typeArguments.isNotEmpty() /*|| constructorOwner.valueParameters.isEmpty()*/)
        ) {
            val substitutor = (candidate.originScope as? FirClassSubstitutionScope)?.substitutor
            var currentTypeArgumentIndex = 0

            refinedTypeArguments = buildList {
                for (typeParameter in owner.typeParameters) {
                    val typeParameterConeType = if (substitutor != null) {
                        substitutor.substituteOrNull(typeParameter.toConeType())
                    } else {
                        typeParameter.toConeType()
                    }

                    val newTypeArgument = typeParameterConeType?.let {
                        buildTypeProjectionWithVariance {
                            typeRef = it.toFirResolvedTypeRef()
                            variance = Variance.INVARIANT
                        }
                    } ?: typeArguments.elementAtOrNull(currentTypeArgumentIndex++)
                    addIfNotNull(newTypeArgument)
                }
                // Add remaining unprocessed type arguments (they are excess and should cause an `WRONG_NUMBER_OF_TYPE_ARGUMENTS` error)
                addAll(typeArguments.drop(currentTypeArgumentIndex + 1))
            }
            desiredTypeParameterCount = currentTypeArgumentIndex
        } else {
            refinedTypeArguments = typeArguments
            desiredTypeParameterCount = owner.typeParameters.size
        }

        return desiredTypeParameterCount to refinedTypeArguments
    }

    private fun computeDefaultMappingForRawTypeMember(
        owner: FirTypeParameterRefsOwner,
        context: ResolutionContext
    ): TypeArgumentMapping.Mapped {
        // There might be some minor inconsistencies where in K2, there might be a raw type, while in K1, there was a regular flexible type
        // And in that case for K2 we would start a regular inference process leads to TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER because raw scopes
        // don't leave type variables there (see KT-54526)
        // Also, it might be a separate feature of K2, because even in cases where both compilers had a raw type, it's convenient not to
        // require explicit type arguments for the places where it doesn't make sense
        // (See `generic1.foo(w)` call in testData/diagnostics/tests/platformTypes/rawTypes/noTypeArgumentsForRawScopedMembers.fir.kt)
        val resultArguments = owner.typeParameters.map { typeParameterRef ->
            buildTypeProjectionWithVariance {
                typeRef =
                    ConeTypeIntersector.intersectTypes(
                        context.typeContext, typeParameterRef.symbol.resolvedBounds.map { it.coneType }
                    ).toFirResolvedTypeRef()
                variance = Variance.INVARIANT
            }
        }
        return TypeArgumentMapping.Mapped(resultArguments)
    }
}

internal object NoTypeArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (callInfo.typeArguments.isNotEmpty()) {
            sink.yieldDiagnostic(InapplicableCandidate)
        }
        candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
    }
}

internal object InitializeEmptyArgumentMap : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        candidate.initializeArgumentMapping(arguments = emptyList(), argumentMapping = LinkedHashMap())
    }
}
