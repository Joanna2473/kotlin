/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.LOGGING
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.cli.js.K2JSCompiler.K2JSCompilerPerformanceManager
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.unwrap
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import java.io.File


// ============================== artifacts ==============================
// ============================== frontend ==============================
// ============================== fir2ir ==============================
// ============================== klib ==============================
// ============================== backend ==============================
// ============================== entrypoint ==============================

object JsCliPipeline {
    private fun runPipeline(argumentsInput: ArgumentsPipelineArtifact<K2JSCompilerArguments>): ExitCode {
        val configurationOutput = JsConfigurationStep.execute(argumentsInput).unwrap { return it.code }
        val configuration = configurationOutput.configuration
        val arguments = argumentsInput.arguments
        val messageCollector = configuration.messageCollector
        val klibOutput = if (arguments.includes == null) {
            val frontendOutput = JsFrontendPipelineStep.execute(configurationOutput).unwrap { return it.code }
            val diagnosticCollector = frontendOutput.diagnosticCollector
            configuration.checkDiagnostics(diagnosticCollector, reportEverything = false)?.let { return it }

            val fir2IrOutput = JsFir2IrPipelineStep.execute(frontendOutput).unwrap { return it.code }
            configuration.checkDiagnostics(diagnosticCollector, alwaysReportDiagnosticNames = true, reportEverything = false)?.let { return it }

            val klibOutput = JsKlibPipelineStep.execute(fir2IrOutput).unwrap { return it.code }
            configuration.checkDiagnostics(diagnosticCollector, alwaysReportDiagnosticNames = true, reportEverything = true)?.let { return it }
            if (!arguments.irProduceJs) return OK
            klibOutput
        } else {
            val environmentForJS = KotlinCoreEnvironment.Companion.createForProduction(
                configurationOutput.rootDisposable,
                configuration,
                EnvironmentConfigFiles.JS_CONFIG_FILES
            )
            JsKlibPipelineArtifact(
                configuration.computeOutputKlibPath(),
                sourceModule = null,
                project = environmentForJS.project,
                argumentsInput.diagnosticCollector,
                configuration
            )
        }

        messageCollector.report(INFO, "Produce executable: ${arguments.outputDir}")
        messageCollector.report(INFO, "Cache directory: ${arguments.cacheDirectory}")

        JsBackendPipelineStep.execute(klibOutput).unwrap { return it.code }
        return OK
    }

    fun CompilerConfiguration.checkDiagnostics(
        diagnosticCollector: BaseDiagnosticsCollector,
        reportEverything: Boolean,
        alwaysReportDiagnosticNames: Boolean = false,
    ): ExitCode? {
        if (reportEverything || diagnosticCollector.hasErrors) {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
                diagnosticCollector, messageCollector,
                alwaysReportDiagnosticNames || renderDiagnosticInternalName
            )
        }
        if (diagnosticCollector.hasErrors) {
            return COMPILATION_ERROR
        }
        return null
    }

    fun execute(
        arguments: K2JSCompilerArguments,
        services: Services,
        originalMessageCollector: MessageCollector,
    ): ExitCode {
        val canceledStatus = services[CompilationCanceledStatus::class.java]
        ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(canceledStatus)
        val rootDisposable = Disposer.newDisposable("Disposable for ${CLICompiler::class.simpleName}.execImpl")
        setIdeaIoUseFallback()
        val performanceManager = createPerformanceManager()
        if (arguments.reportPerf || arguments.dumpPerf != null) {
            performanceManager.enableCollectingPerformanceStatistics()
        }

        val messageCollector = GroupingMessageCollector(
            originalMessageCollector,
            arguments.allWarningsAsErrors,
            arguments.reportAllWarnings
        )
        val argumentsInput = ArgumentsPipelineArtifact(
            arguments,
            services,
            rootDisposable,
            messageCollector,
            performanceManager
        )

        fun reportException(e: Throwable): ExitCode {
            MessageCollectorUtil.reportException(messageCollector, e)
            return if (e is OutOfMemoryError || e.hasOOMCause()) OOM_ERROR else INTERNAL_ERROR
        }

        fun reportCompilationCanceled(e: CompilationCanceledException): ExitCode {
            messageCollector.reportCompilationCancelled(e)
            return OK
        }

        return try {
            val code = runPipeline(argumentsInput)
            performanceManager.notifyCompilationFinished()
            if (arguments.reportPerf) {
                messageCollector.report(LOGGING, "PERF: " + performanceManager.getTargetInfo())
                for (measurement in performanceManager.getMeasurementResults()) {
                    messageCollector.report(LOGGING, "PERF: " + measurement.render(), null)
                }
            }

            if (arguments.dumpPerf != null) {
                performanceManager.dumpPerformanceReport(File(arguments.dumpPerf!!))
            }

            if (messageCollector.hasErrors()) COMPILATION_ERROR else code
        } catch (_: CompilationErrorException) {
            COMPILATION_ERROR
        } catch (e: RuntimeException) {
            when (val cause = e.cause) {
                is CompilationCanceledException -> reportCompilationCanceled(cause)
                else -> reportException(e)
            }
        } catch (t: Throwable) {
            reportException(t)
        } finally {
            messageCollector.flush()
            Disposer.dispose(rootDisposable)
        }
    }

    private fun createPerformanceManager(): CommonCompilerPerformanceManager {
        return K2JSCompilerPerformanceManager()
    }
}
