/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.js.Ir2JsTransformer
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.CompilerPipelineStep
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.StepStatus
import org.jetbrains.kotlin.cli.pipeline.invokeAsPhase
import org.jetbrains.kotlin.cli.pipeline.toErrorStatus
import org.jetbrains.kotlin.cli.pipeline.toOkStatus
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.getJsPhases
import org.jetbrains.kotlin.js.config.callMain
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.js.config.moduleKind
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.js.config.outputName
import org.jetbrains.kotlin.js.config.tsCompilationStrategy
import org.jetbrains.kotlin.js.config.wasmCompilation
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.File

object JsBackendPipelinePhase : PipelinePhase<JsKlibPipelineArtifact, JsBackendPipelineArtifact>(
    name = "JsBackendPipelinePhase",
    step = JsBackendPipelineStep,
    preActions = setOf(PerformanceNotifications.GenerationStarted),
    postActions = setOf(PerformanceNotifications.GenerationFinished, CheckCompilationErrors),
)
object JsBackendPipelineStep : CompilerPipelineStep<JsKlibPipelineArtifact, JsBackendPipelineArtifact>() {
    override fun execute(input: JsKlibPipelineArtifact): StepStatus<JsBackendPipelineArtifact> {
        val (_, sourceModule, project, _, configuration) = input
        val messageCollector = configuration.messageCollector
        messageCollector.report(INFO, "Produce executable: ${configuration.outputDir}")
        messageCollector.report(INFO, "Cache directory: null") // arguments.cacheDirectory was here
        val module = sourceModule ?: run {
            val includes = configuration.includes!!
            val includesPath = File(includes).canonicalPath
            val mainLibPath = configuration.libraries.find { File(it).canonicalPath == includesPath }
                ?: error("No library with name $includes ($includesPath) found")
            val kLib = MainModule.Klib(mainLibPath)
            ModulesStructure(
                project,
                kLib,
                configuration,
                configuration.libraries,
                configuration.friendLibraries
            ).also {
                K2JSCompiler.Companion.runStandardLibrarySpecialCompatibilityChecks(
                    it.allDependencies,
                    isWasm = configuration.wasmCompilation,
                    messageCollector
                )
            }
        }

        val start = System.currentTimeMillis()
        val phaseConfig = createPhaseConfig(getJsPhases(configuration), configuration, messageCollector)
        return try {
            val mainCallArguments = when {
                configuration.callMain -> emptyList<String>()
                else -> null
            }
            val ir2JsTransformer = Ir2JsTransformer(configuration, module, phaseConfig, messageCollector, mainCallArguments)
            val outputs = ir2JsTransformer.compileAndTransformIrNew()

            messageCollector.report(CompilerMessageSeverity.INFO, "Executable production duration: ${System.currentTimeMillis() - start}ms")
            val outputDir = configuration.outputDir!!
            outputs.writeAll(
                outputDir,
                configuration.outputName!!,
                configuration.tsCompilationStrategy!!,
                configuration.moduleName!!,
                configuration.moduleKind!!,
            )
            return JsBackendPipelineArtifact(outputDir, outputs).toOkStatus()
        } catch (e: CompilationException) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                e.stackTraceToString(),
                CompilerMessageLocation.Companion.create(
                    path = e.path,
                    line = e.line,
                    column = e.column,
                    lineContent = e.content
                )
            )
            ExitCode.INTERNAL_ERROR.toErrorStatus()
        }
    }
}
