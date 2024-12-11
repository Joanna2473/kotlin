/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.JVM_INLINE_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.java.jvmTargetProvider
import org.jetbrains.kotlin.lexer.KtTokens

object FirJvmInlineApplicabilityChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val targetJvmTarget = context.session.jvmTargetProvider?.jvmTarget
        val isValhallaSupportedInJdk = targetJvmTarget != null && targetJvmTarget.majorVersion >= JvmTarget.JVM_23.majorVersion
        val isJvmPreviewEnabled = context.languageVersionSettings.getFlag(JvmAnalysisFlags.enableJvmPreview)
        val isValhallaSupportEnabled = context.languageVersionSettings.supportsFeature(LanguageFeature.ValhallaValueClasses)
        val annotation = declaration.getAnnotationByClassId(JVM_INLINE_ANNOTATION_CLASS_ID, context.session)
        if (annotation != null && !(declaration.isInline && declaration.getModifier(KtTokens.VALUE_KEYWORD) != null)) {
            // only report if value keyword does not exist, this includes the deprecated inline class syntax
            reporter.reportOn(annotation.source, FirJvmErrors.JVM_INLINE_WITHOUT_VALUE_CLASS, context)
        } else if (annotation == null && declaration.isInline && !declaration.isExpect) {
            // only report if value keyword exists, this ignores the deprecated inline class syntax
            val valueKeyword = declaration.getModifier(KtTokens.VALUE_KEYWORD)?.source ?: return
            if (!isValhallaSupportEnabled) {
                reporter.reportOn(valueKeyword, FirJvmErrors.VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION, context)
            } else if (!isValhallaSupportedInJdk) {
                val actualJvmTarget = targetJvmTarget?.description ?: return
                reporter.reportOn(valueKeyword, FirJvmErrors.VALHALLA_VALUE_CLASS_ON_OLD_JVM_TARGET, actualJvmTarget, context)
            } else if (!isJvmPreviewEnabled) {
                reporter.reportOn(valueKeyword, FirJvmErrors.VALHALLA_VALUE_CLASS_WITHOUT_JVM_PREVIEW, context)
            }
        }
    }
}
