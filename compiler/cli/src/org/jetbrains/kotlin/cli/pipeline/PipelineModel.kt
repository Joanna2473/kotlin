/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult

// ============================== configuration ==============================

abstract class ConfigurationFiller<in A : CommonCompilerArguments, C> {
    abstract fun fillConfiguration(arguments: A, configuration: CompilerConfiguration, context: C): ExitCode
}

// ============================== artifacts ==============================

sealed class StepStatus<out A : PipelineArtifact> {
    class Ok<out A : PipelineArtifact>(val result: A) : StepStatus<A>()

    open class Error(val code: ExitCode) : StepStatus<Nothing>()
    class NoSources : Error(ExitCode.COMPILATION_ERROR)
}

val <T : PipelineArtifact> StepStatus<T>.resultOrFail: T
    get() = (this as StepStatus.Ok<T>).result

abstract class PipelineArtifact

data class ArgumentsPipelineArtifact<out A : CommonCompilerArguments>(
    val arguments: A,
    val services: Services,
    val rootDisposable: Disposable,
    val messageCollector: GroupingMessageCollector,
    val performanceManager: CommonCompilerPerformanceManager,
) : PipelineArtifact() {
    val diagnosticCollector: BaseDiagnosticsCollector = DiagnosticReporterFactory.createPendingReporter(messageCollector)
}

data class ConfigurationPipelineArtifact(
    val configuration: CompilerConfiguration,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val rootDisposable: Disposable,
) : PipelineArtifact()

abstract class FrontendPipelineArtifact : PipelineArtifact() {
    abstract val result: FirResult
}

abstract class Fir2IrPipelineArtifact : PipelineArtifact() {
    abstract val result: Fir2IrActualizedResult
}

// ============================== steps ==============================

// C for Context
abstract class CompilerPipelineStep<in I : PipelineArtifact, out O : PipelineArtifact> {
    abstract fun execute(input: I): StepStatus<O>
}

// ============================== utils ==============================

class PipelineStepException : RuntimeException()

fun <A : PipelineArtifact> A.toOkStatus(): StepStatus.Ok<A> {
    return StepStatus.Ok(this)
}

fun ExitCode.toErrorStatus(): StepStatus.Error {
    return StepStatus.Error(this)
}

inline fun <A : PipelineArtifact> StepStatus<A>.unwrap(onError: (StepStatus.Error) -> A): A {
    return when (this) {
        is StepStatus.Ok -> result
        is StepStatus.Error -> onError(this)
    }
}
