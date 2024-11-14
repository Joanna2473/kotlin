/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.CompilerPipelineStep
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.StepStatus
import org.jetbrains.kotlin.cli.pipeline.toOkStatus

object JsKlibLoadingPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, JsKlibPipelineArtifact>(
    name = "JsKlibLoadingPipelinePhase",
    step = JsKlibLoadingPipelineArtifact,
    postActions = setOf(CheckCompilationErrors)
)

object JsKlibLoadingPipelineArtifact : CompilerPipelineStep<ConfigurationPipelineArtifact, JsKlibPipelineArtifact>() {
    override fun execute(input: ConfigurationPipelineArtifact): StepStatus<JsKlibPipelineArtifact> {
        val configuration = input.configuration
        val environmentForJS = KotlinCoreEnvironment.Companion.createForProduction(
            input.rootDisposable,
            configuration,
            EnvironmentConfigFiles.JS_CONFIG_FILES
        )
        return JsKlibPipelineArtifact(
            configuration.computeOutputKlibPath(),
            sourceModule = null,
            project = environmentForJS.project,
            input.diagnosticCollector,
            configuration
        ).toOkStatus()
    }
}
