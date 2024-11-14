/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.extensions.ShellExtension
import org.jetbrains.kotlin.cli.common.freeArgsForScript
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.scriptMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.config.expressionToEvaluate
import org.jetbrains.kotlin.config.messageCollector

object JvmScriptPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, JvmScriptPipelineArtifact>(
    name = "JvmScriptPipelinePhase",
    step = JvmScriptPipelineStep
)

object JvmScriptPipelineStep : CompilerPipelineStep<ConfigurationPipelineArtifact, JvmScriptPipelineArtifact>() {
    override fun execute(input: ConfigurationPipelineArtifact): StepStatus<JvmScriptPipelineArtifact> {
        val (configuration, _, rootDisposable) = input
        val messageCollector = configuration.messageCollector
        if (configuration.scriptMode && configuration.freeArgsForScript.isEmpty()) {
            messageCollector.report(ERROR, "Specify script source path to evaluate")
            return COMPILATION_ERROR.toErrorStatus()
        }

        val projectEnvironment =
            KotlinCoreEnvironment.ProjectEnvironment(
                rootDisposable,
                KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(rootDisposable, configuration),
                configuration
            )
        projectEnvironment.registerExtensionsFromPlugins(configuration)

        val result = if (configuration.scriptMode || configuration.expressionToEvaluate != null) {
            val argumentsStub = K2JVMCompilerArguments().apply {
                script = true
            }
            val scriptingEvaluator = ScriptEvaluationExtension.getInstances(projectEnvironment.project)
                .find { it.isAccepted(argumentsStub) }
            if (scriptingEvaluator == null) {
                messageCollector.report(ERROR, "Unable to evaluate script, no scripting plugin loaded")
                return COMPILATION_ERROR.toErrorStatus()
            }
            scriptingEvaluator.eval(configuration, projectEnvironment)
        } else {
            // arguments are unused in the ShellExtension anyway
            val argumentsStub = K2JVMCompilerArguments()
            val shell = ShellExtension.getInstances(projectEnvironment.project).find { it.isAccepted(argumentsStub) }
            if (shell == null) {
                messageCollector.report(ERROR, "Unable to run REPL, no scripting plugin loaded")
                return COMPILATION_ERROR.toErrorStatus()
            }
            shell.run(argumentsStub, configuration, projectEnvironment)
        }
        return JvmScriptPipelineArtifact(result).toOkStatus()
    }
}
