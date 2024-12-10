/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.backend.js.TsCompilationStrategy
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

object JsBackendPipelinePhase : WebBackendPipelinePhase<JsBackendPipelineArtifact>("JsBackendPipelinePhase") {
    override fun compileWithIC(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
    ): JsBackendPipelineArtifact? {
        val outputs = compileWithIC(
            icCaches,
            configuration,
            configuration.moduleKind!!,
            configuration.moduleName!!,
            configuration.outputDir!!,
            configuration.outputName!!,
            configuration.granularity!!,
            configuration.tsCompilationStrategy!!
        )
        return JsBackendPipelineArtifact(outputs, configuration.outputDir!!, configuration)
    }

    /**
     * This method is shared between K2 phased pipeline and `K2JsCompilerImpl.compileWithIC` for K1 CLI
     */
    fun compileWithIC(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
        moduleKind: ModuleKind,
        moduleName: String,
        outputDir: File,
        outputName: String,
        granularity: JsGenerationGranularity,
        tsStrategy: TsCompilationStrategy
    ): CompilationOutputs {
        val messageCollector = configuration.messageCollector
        val beforeIc2Js = System.currentTimeMillis()

        val jsArtifacts = icCaches.artifacts.filterIsInstance<JsModuleArtifact>()
        val jsExecutableProducer = JsExecutableProducer(
            mainModuleName = moduleName,
            moduleKind = moduleKind,
            sourceMapsInfo = SourceMapsInfo.from(configuration),
            caches = jsArtifacts,
            relativeRequirePath = true
        )
        val (outputs, rebuiltModules) = jsExecutableProducer.buildExecutable(granularity, outJsProgram = false)
        outputs.writeAll(outputDir, outputName, tsStrategy, moduleName, moduleKind)

        messageCollector.report(INFO, "Executable production duration (IC): ${System.currentTimeMillis() - beforeIc2Js}ms")
        for ((event, duration) in jsExecutableProducer.getStopwatchLaps()) {
            messageCollector.report(INFO, "  $event: ${(duration / 1e6).toInt()}ms")
        }

        for (module in rebuiltModules) {
            messageCollector.report(INFO, "IC module builder rebuilt JS for module [${File(module).name}]")
        }
        return outputs
    }

    override fun compileWithoutIC(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        mainCallArguments: List<String>?,
    ): JsBackendPipelineArtifact? {
        val messageCollector = configuration.messageCollector
        val ir2JsTransformer = Ir2JsTransformer(configuration, module, messageCollector, mainCallArguments)
        val outputs = compileWithoutIC(
            messageCollector,
            ir2JsTransformer,
            configuration.moduleKind!!,
            configuration.moduleName!!,
            configuration.outputDir!!,
            configuration.outputName!!,
            configuration.tsCompilationStrategy!!
        ) ?: return null
        return JsBackendPipelineArtifact(outputs, configuration.outputDir!!, configuration)
    }

    /**
     * This method is shared between K2 phased pipeline and `K2JsCompilerImpl.compileNoIC` for K1 CLI
     */
    fun compileWithoutIC(
        messageCollector: MessageCollector,
        ir2JsTransformer: Ir2JsTransformer,
        moduleKind: ModuleKind,
        moduleName: String,
        outputDir: File,
        outputName: String,
        tsStrategy: TsCompilationStrategy
    ): CompilationOutputs? {
        val start = System.currentTimeMillis()
        try {
            val outputs = ir2JsTransformer.compileAndTransformIrNew()
            messageCollector.report(INFO, "Executable production duration: ${System.currentTimeMillis() - start}ms")
            outputs.writeAll(outputDir, outputName, tsStrategy, moduleName, moduleKind)
            return outputs
        } catch (e: CompilationException) {
            messageCollector.report(
                ERROR,
                e.stackTraceToString(),
                CompilerMessageLocation.create(
                    path = e.path,
                    line = e.line,
                    column = e.column,
                    lineContent = e.content
                )
            )
            return null
        }
    }
}

