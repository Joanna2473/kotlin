/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler.K2JVMCompilerPerformanceManager
import org.jetbrains.kotlin.cli.pipeline.*


object JvmCliPipeline : AbstractCliPipeline<K2JVMCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2JVMCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, *> {
        return when {
            arguments.scriptingModeEnabled -> createScriptPipeline()
            else -> createRegularPipeline()
        }
    }

    private fun createRegularPipeline(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, JvmBinaryPipelineArtifact> =
        JvmConfigurationPipelinePhase then
                JvmFrontendPipelinePhase then
                JvmFir2IrPipelinePhase then
                JvmBackendPipelinePhase

    private fun createScriptPipeline(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JVMCompilerArguments>, JvmScriptPipelineArtifact> =
        JvmConfigurationPipelinePhase then
                JvmScriptPipelinePhase

    private val K2JVMCompilerArguments.scriptingModeEnabled: Boolean
        get() = buildFile == null &&
                !version &&
                !allowNoSourceFiles &&
                (script || expression != null || freeArgs.isEmpty())


    override fun createPerformanceManager(): CommonCompilerPerformanceManager {
        return K2JVMCompilerPerformanceManager()
    }
}
