/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import org.jetbrains.kotlin.cli.common.perfManager
import org.jetbrains.kotlin.cli.js.klib.transformFirToIr
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase

object JsFir2IrPipelinePhase : PipelinePhase<JsFrontendPipelineArtifact, JsFir2IrPipelineArtifact>(
    name = "JsFir2IrPipelinePhase",
    preActions = setOf(PerformanceNotifications.IrTranslationStarted),
    postActions = setOf(PerformanceNotifications.IrTranslationFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JsFrontendPipelineArtifact): JsFir2IrPipelineArtifact? {
        val (analyzedOutput, configuration, diagnosticsReporter, moduleStructure) = input
        val performanceManager = configuration.perfManager
        performanceManager?.notifyIRTranslationStarted()
        val fir2IrActualizedResult = transformFirToIr(moduleStructure, analyzedOutput.output, diagnosticsReporter)
        performanceManager?.notifyIRTranslationFinished()
        return JsFir2IrPipelineArtifact(
            fir2IrActualizedResult,
            analyzedOutput,
            configuration,
            diagnosticsReporter,
            moduleStructure
        )
    }
}
