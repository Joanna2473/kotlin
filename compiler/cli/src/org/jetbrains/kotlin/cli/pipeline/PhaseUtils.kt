/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class PipelineContext(
    val messageCollector: MessageCollector,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val performanceManager: CommonCompilerPerformanceManager,
    val renderDiagnosticInternalName: Boolean
) : LoggingContext {
    override var inVerbosePhase: Boolean = false
}

abstract class PipelinePhase<I : PipelineArtifact, O : PipelineArtifact>(
    name: String,
    val step: CompilerPipelineStep<I, O>,
    preActions: Set<Action<I, PipelineContext>> = emptySet(),
    postActions: Set<Action<Pair<I, O>, PipelineContext>> = emptySet(),
) : SimpleNamedCompilerPhase<PipelineContext, I, O>(
    name = name,
    preactions = preActions,
    postactions = postActions
) {
    override fun phaseBody(context: PipelineContext, input: I): O {
        return step.invokeAsPhase(input)
    }

    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<I>,
        context: PipelineContext,
        input: I,
    ): O {
        shouldNotBeCalled()
    }
}

object CheckCompilationErrors : Action<Pair<PipelineArtifact, PipelineArtifact>, PipelineContext> {
    override fun invoke(
        state: ActionState,
        artifacts: Pair<PipelineArtifact, PipelineArtifact>,
        c: PipelineContext,
    ) {
        if (c.diagnosticCollector.hasErrors) {
            throw PipelineStepException()
        }
    }

    fun reportDiagnosticsToMessageCollector(c: PipelineContext) {
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
            c.diagnosticCollector, c.messageCollector,
            c.renderDiagnosticInternalName
        )
    }
}

object PerformanceNotifications {
    object AnalysisStarted : AbstractStartedNotification(CommonCompilerPerformanceManager::notifyAnalysisStarted)
    object AnalysisFinished : AbstractFinishedNotification(CommonCompilerPerformanceManager::notifyAnalysisFinished)

    object IrTranslationStarted : AbstractStartedNotification(CommonCompilerPerformanceManager::notifyIRTranslationStarted)
    object IrTranslationFinished : AbstractFinishedNotification(CommonCompilerPerformanceManager::notifyIRTranslationFinished)

    object IrLoweringsStarted : AbstractStartedNotification(CommonCompilerPerformanceManager::notifyIRLoweringStarted)
    object IrLoweringsFinished : AbstractFinishedNotification(CommonCompilerPerformanceManager::notifyIRLoweringFinished)

    object GenerationStarted : AbstractStartedNotification(CommonCompilerPerformanceManager::notifyGenerationStarted)
    object GenerationFinished : AbstractFinishedNotification(CommonCompilerPerformanceManager::notifyGenerationFinished)

    sealed class AbstractStartedNotification(
        val notify: CommonCompilerPerformanceManager.() -> Unit
    ) : Action<PipelineArtifact, PipelineContext> {
        override fun invoke(
            state: ActionState,
            input: PipelineArtifact,
            c: PipelineContext,
        ) {
            c.performanceManager.notify()
        }
    }

    sealed class AbstractFinishedNotification(
        val notify: CommonCompilerPerformanceManager.() -> Unit
    ) : Action<Pair<PipelineArtifact, PipelineArtifact>, PipelineContext> {
        override fun invoke(
            state: ActionState,
            artifacts: Pair<PipelineArtifact, PipelineArtifact>,
            c: PipelineContext,
        ) {
            c.performanceManager.notify()
        }
    }
}

fun <I : PipelineArtifact, O : PipelineArtifact> CompilerPipelineStep<I, O>.invokeAsPhase(input: I): O {
    val output = this.execute(input)
    return when (output) {
        is StepStatus.Ok -> output.result
        is StepStatus.Error -> throw PipelineStepException()
    }
}
