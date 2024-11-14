/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler.K2JSCompilerPerformanceManager
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext

object JsCliPipeline : AbstractCliPipeline<K2JSCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2JSCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, *> {
        return when {
            arguments.includes != null -> createCodeGenerationPhase()
            arguments.irProduceJs -> createFullPipelinePhase()
            else -> createKlibSerializationPhase()
        }
    }

    private fun createKlibSerializationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, JsKlibPipelineArtifact> {
        return JsConfigurationPhase then
                JsFrontendPipelinePhase then
                JsFir2IrPipelinePhase then
                JsKlibPipelinePhase
    }

    private fun createCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, JsBackendPipelineArtifact> {
        return JsConfigurationPhase then
                JsKlibLoadingPipelinePhase then
                JsBackendPipelinePhase
    }

    private fun createFullPipelinePhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, JsBackendPipelineArtifact> {
        return createKlibSerializationPhase() then
                JsBackendPipelinePhase
    }

    override fun createPerformanceManager(): CommonCompilerPerformanceManager {
        return K2JSCompilerPerformanceManager()
    }
}
