/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.cli.js.klib.serializeFirKlib
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.CompilerPipelineStep
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.StepStatus
import org.jetbrains.kotlin.cli.pipeline.toOkStatus
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.backend.js.JsPreSerializationLoweringPhasesProvider
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.wasm.config.wasmTarget

object JsKlibPipelinePhase : PipelinePhase<JsFir2IrPipelineArtifact, JsKlibPipelineArtifact>(
    name = "JsKlibPipelinePhase",
    step = JsKlibPipelineStep,
    postActions = setOf(CheckCompilationErrors)
)

object JsKlibPipelineStep : CompilerPipelineStep<JsFir2IrPipelineArtifact, JsKlibPipelineArtifact>() {
    override fun execute(input: JsFir2IrPipelineArtifact): StepStatus<JsKlibPipelineArtifact> {
        val (fir2IrResult, firOutput, configuration, diagnosticCollector, moduleStructure) = input
        val phaseConfig = createPhaseConfig(
            JsPreSerializationLoweringPhasesProvider.lowerings(configuration),
            configuration,
            configuration.messageCollector,
        )

        val transformedResult = PhaseEngine(
            phaseConfig,
            PhaserState(),
            PreSerializationLoweringContext(fir2IrResult.irBuiltIns, configuration),
        ).runPreSerializationLoweringPhases(fir2IrResult, JsPreSerializationLoweringPhasesProvider, configuration)

        val outputKlibPath = configuration.computeOutputKlibPath()
        serializeFirKlib(
            moduleStructure = moduleStructure,
            firOutputs = firOutput.output,
            fir2IrActualizedResult = transformedResult,
            outputKlibPath = outputKlibPath,
            nopack = configuration.produceKlibDir,
            messageCollector = configuration.messageCollector,
            diagnosticsReporter = diagnosticCollector,
            jsOutputName = configuration.perModuleOutputName,
            useWasmPlatform = configuration.wasmCompilation,
            wasmTarget = configuration.wasmTarget
        )
        return JsKlibPipelineArtifact(
            outputKlibPath,
            moduleStructure,
            moduleStructure.project,
            diagnosticCollector,
            configuration
        ).toOkStatus()
    }
}


fun CompilerConfiguration.computeOutputKlibPath(): String {
    return if (produceKlibFile) {
        outputDir!!.resolve("${outputName!!}.klib").normalize().absolutePath
    } else {
        outputDir!!.absolutePath
    }
}
