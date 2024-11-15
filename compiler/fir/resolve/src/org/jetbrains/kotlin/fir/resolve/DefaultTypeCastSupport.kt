/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeVariable
import org.jetbrains.kotlin.fir.types.TypeCastSupport
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

// Not an `object` to prevent IJ from suggesting imports of
// [DefaultTypeCastSupport.createStaticallyKnownSubtype], as
// it's better to access it via the actual session component.
class DefaultTypeCastSupport : TypeCastSupport {
    /**
     * Remember that we are trying to cast something of type `supertype` to `subtype`.

     * Since at runtime we can only check the class (type constructor), the rest of the subtype should be known statically, from supertype.
     * This method reconstructs all static information that can be obtained from supertype.

     * Example 1:
     * supertype = Collection
     * subtype = List<...>
     * result = List, all arguments are inferred

     * Example 2:
     * supertype = Any
     * subtype = List<...>
     * result = List<*>, some arguments were not inferred, replaced with '*'
     */
    override fun createStaticallyKnownSubtype(
        supertype: ConeKotlinType,
        subTypeClassSymbol: FirRegularClassSymbol,
        isSubTypeMarkedNullable: Boolean,
        attributes: ConeAttributes,
        session: FirSession,
    ): ConeKotlinType = subTypeClassSymbol.constructType(
        typeArguments = findStaticallyKnownSubtypeArguments(supertype, subTypeClassSymbol, session),
        isMarkedNullable = isSubTypeMarkedNullable,
        attributes = attributes,
    )

    private fun findStaticallyKnownSubtypeArguments(
        supertype: ConeKotlinType,
        subTypeClassSymbol: FirRegularClassSymbol,
        session: FirSession,
    ): Array<ConeTypeProjection> {
        val constraintSystem = session.inferenceComponents.createConstraintSystem()

        val subTypeVariables = subTypeClassSymbol.typeParameterSymbols.map {
            val variable = ConeTypeVariable(it.name.asString(), it.toLookupTag())
            constraintSystem.registerVariable(variable)
            variable
        }
        val subType = subTypeClassSymbol.constructType(subTypeVariables.map { it.defaultType }.toTypedArray())

        constraintSystem.addSubtypeConstraint(subType, supertype, SimpleConstraintSystemConstraintPosition)

        if (constraintSystem.hasContradiction) {
            // The cast is impossible, we shouldn't really be here
            return subTypeVariables.map { ConeStarProjection }.toTypedArray()
        }

        val substitutorMap = mutableMapOf<TypeVariableMarker, ConeTypeProjection>()

        for ((_, constraints) in constraintSystem.currentStorage().notFixedTypeVariables) {
            val variable = constraints.typeVariable

            constraints.constraints.firstOrNull { it.kind == ConstraintKind.EQUALITY }?.let {
                substitutorMap[variable] = it.type as ConeKotlinType
                continue
            }

            val (upperConstraints, lowerConstraints) = constraints.constraints.partition { it.kind == ConstraintKind.UPPER }

            when {
                upperConstraints.isNotEmpty() && lowerConstraints.isEmpty() -> substitutorMap[variable] = session.typeContext
                    .intersectTypes(upperConstraints.map { it.type })
                    .let { ConeKotlinTypeProjectionOut(it) }
                upperConstraints.isEmpty() && lowerConstraints.isNotEmpty() -> substitutorMap[variable] = session.typeContext
                    .commonSuperType(lowerConstraints.map { it.type })
                    .let { ConeKotlinTypeProjectionIn(it as ConeKotlinType) }
                // If both are empty, then this is correct, if both are not, then there's no
                // obvious way how we should fix the variable, so we choose to be conservative.
                else -> substitutorMap[variable] = ConeStarProjection
            }
        }

        return subTypeVariables.map { substitutorMap[it] ?: error("Variable $it has not been fixed to anything") }.toTypedArray()
    }
}
