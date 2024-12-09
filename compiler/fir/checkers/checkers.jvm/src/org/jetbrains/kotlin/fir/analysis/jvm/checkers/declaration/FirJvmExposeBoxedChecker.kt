/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration


import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.ExplicitApiMode
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.java.findJvmNameValue
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name

object FirJvmExposeBoxedChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val jvmExposeBoxedAnnotation =
            declaration.getAnnotationByClassId(JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_CLASS_ID, context.session)

        if (jvmExposeBoxedAnnotation != null) {
            checkJvmExposeBoxedAnnotation(jvmExposeBoxedAnnotation, declaration, reporter, context)
        } else if (context.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_2_2 &&
            context.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode) != ExplicitApiMode.DISABLED
        ) {
            checkExplicitApiMode(declaration, context, reporter)
        }
    }

    private fun checkJvmExposeBoxedAnnotation(
        jvmExposeBoxedAnnotation: FirAnnotation,
        declaration: FirDeclaration,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        val name = jvmExposeBoxedAnnotation.findArgumentByName(JvmStandardClassIds.Annotations.ParameterNames.jvmExposeBoxedName)
        val exposedParam = jvmExposeBoxedAnnotation.findArgumentByName(JvmStandardClassIds.Annotations.ParameterNames.jvmExposeBoxedExpose)
        val exposed = (exposedParam as? FirLiteralExpression)?.value as? Boolean != false

        if (name != null) {
            if (!exposed) {
                reporter.reportOn(name.source, FirJvmErrors.JVM_EXPOSE_BOXED_REDUNDANT_NAME, context)
            }

            if (declaration.isContainer()) {
                reporter.reportOn(name.source, FirJvmErrors.INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME, context)
            }

            val value = (name as? FirLiteralExpression)?.value as? String
            if (value != null && !Name.isValidIdentifier(value)) {
                reporter.reportOn(name.source, FirJvmErrors.ILLEGAL_JVM_NAME, context)
            }
        }

        if (declaration is FirCallableDeclaration) {
            if (!declaration.isWithInlineClass(context.session)) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.USELESS_JVM_EXPOSE_BOXED, context)
            } else if (exposed && name == null && !declaration.isMangledOrWithResult(context.session)) {
                reportMissingName(declaration, jvmExposeBoxedAnnotation, reporter, context)
            }

            if (!declaration.canBeOverloadedByExposed(context.session)) {
                checkJvmNameHasDifferentName(name, declaration, reporter, jvmExposeBoxedAnnotation, context)
            }

            if (declaration is FirFunction && declaration.typeParameters.any { it.symbol.isReified }) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.USELESS_JVM_EXPOSE_BOXED, context)
            }

            if (declaration is FirFunction && declaration.isSuspend && exposed) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND, context)
            }
        }
    }

    private fun checkExplicitApiMode(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (declaration !is FirCallableDeclaration) return

        if (declaration.isSuspend) return

        if (declaration.isMangled(context.session)) {
            if (context.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode) == ExplicitApiMode.STRICT) {
                reporter.reportOn(declaration.source, FirJvmErrors.JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT, context)
            } else {
                reporter.reportOn(declaration.source, FirJvmErrors.JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT_WARNING, context)
            }
            return
        }

        val inlineClass = declaration.getContainingClass(context.session)
        if (inlineClass?.isInline == true) {
            // Do not duplicate diagnostic on a property
            if (declaration is FirDefaultPropertyGetter || declaration is FirDefaultPropertySetter) return

            if (declaration.isUnderlyingPropertyOfInlineClass(context.session)) return

            if (context.languageVersionSettings.getFlag(AnalysisFlags.explicitApiMode) == ExplicitApiMode.STRICT) {
                reporter.reportOn(declaration.source, FirJvmErrors.JVM_EXPOSE_BOXED_METHOD_MUST_BE_EXPLICIT, context)
            } else {
                reporter.reportOn(declaration.source, FirJvmErrors.JVM_EXPOSE_BOXED_METHOD_MUST_BE_EXPLICIT_WARNING, context)
            }
        }
    }

    // Default getter of underlying value is already exposed, unless it is also inline class,
    // but this is already covered by another diagnostic
    private fun FirCallableDeclaration.isUnderlyingPropertyOfInlineClass(session: FirSession): Boolean {
        if (this !is FirProperty) return false

        val inlineClass = getContainingClass(session) ?: return false
        if (!inlineClass.isInline) return false
        val primaryConstructor = inlineClass.primaryConstructorSymbol(session) ?: return false
        val valueParameterSymbol = primaryConstructor.valueParameterSymbols.firstOrNull() ?: return false
        return valueParameterSymbol == correspondingValueParameterFromPrimaryConstructor
    }

    private fun reportMissingName(
        declaration: FirCallableDeclaration,
        jvmExposeBoxedAnnotation: FirAnnotation,
        reporter: DiagnosticReporter,
        context: CheckerContext,
    ) {
        when (declaration) {
            is FirProperty ->
                if (jvmExposeBoxedAnnotation.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_GETTER) {
                    reporter.reportOn(
                        jvmExposeBoxedAnnotation.source,
                        FirJvmErrors.JVM_EXPOSE_BOXED_REQUIRES_NAMED_GETTER,
                        context
                    )
                }
            is FirFunction ->
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_REQUIRES_NAME, context)
            else -> {} /* do nothing. */
        }
    }

    private fun checkJvmNameHasDifferentName(
        name: FirExpression?,
        declaration: FirDeclaration,
        reporter: DiagnosticReporter,
        jvmExposeBoxedAnnotation: FirAnnotation,
        context: CheckerContext,
    ) {
        if (name != null) {
            val value = (name as? FirLiteralExpression)?.value as? String

            if (value != null && value == declaration.findJvmNameValue()) {
                reporter.reportOn(
                    name.source,
                    FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME,
                    context
                )
            }

            if (declaration is FirFunction && declaration.nameOrSpecialName.asString() == value) {
                reporter.reportOn(jvmExposeBoxedAnnotation.source, FirJvmErrors.JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME, context)
            }
        }
    }

    private fun FirDeclaration.isContainer(): Boolean =
        this is FirFile || this is FirClass || this is FirConstructor || this is FirProperty

    private fun FirCallableDeclaration.isWithInlineClass(session: FirSession): Boolean {
        if (canBeOverloadedByExposed(session)) return true
        if (returnTypeRef.isInline(session)) return true
        return false
    }

    /* Just like [isMangled], but takes into account little quirks like globals, returning inline class. */
    private fun FirCallableDeclaration.isMangledOrWithResult(session: FirSession): Boolean {
        if (canBeOverloadedByExposed(session)) return true
        val containingClass = containingClassLookupTag()?.toRegularClassSymbol(session)
        if (containingClass != null) return returnTypeRef.isInline(session)
        return false
    }

    // If the inline class is not return type, it is safe to name both boxed and unboxed versions the same.
    private fun FirCallableDeclaration.canBeOverloadedByExposed(session: FirSession): Boolean {
        if (receiverParameter?.typeRef?.isInline(session) == true) return true
        if (contextReceivers.any { it.returnTypeRef.isInline(session) }) return true
        if (this is FirFunction && valueParameters.any { it.returnTypeRef.isInline(session) }) return true
        return false
    }

    private fun FirCallableDeclaration.isMangled(session: FirSession): Boolean {
        if (this is FirProperty) return false
        if (receiverParameter?.typeRef?.isInlineClassThatRequiresMangling(session) == true) return true
        if (contextReceivers.any { it.returnTypeRef.isInlineClassThatRequiresMangling(session) }) return true
        if (this is FirFunction && valueParameters.any { it.returnTypeRef.isInlineClassThatRequiresMangling(session) })
            return true
        val containingClass = getContainingClass(session)
        if (containingClass != null) return returnTypeRef.isInlineClassThatRequiresMangling(session)
        return false
    }

    private fun FirCallableDeclaration.getContainingClass(session: FirSession): FirRegularClassSymbol? =
        containingClassLookupTag()?.toRegularClassSymbol(session)

    private fun FirTypeRef.isInline(session: FirSession): Boolean =
        toRegularClassSymbol(session)?.isInline ?: false
}