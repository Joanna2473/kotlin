/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase

sealed class WebKLibLoadingPipelinePhase(name: String) : PipelinePhase<ConfigurationPipelineArtifact, JsLoadedKlibPipelineArtifact>(
    name = name,
    postActions = setOf(CheckCompilationErrors.CheckDiagnosticCollector)
) {
    protected abstract val configFiles: EnvironmentConfigFiles

    override fun executePhase(input: ConfigurationPipelineArtifact): JsLoadedKlibPipelineArtifact? {
        val configuration = input.configuration
        val environmentForJS = KotlinCoreEnvironment.Companion.createForProduction(
            input.rootDisposable,
            configuration,
            configFiles
        )
        return JsLoadedKlibPipelineArtifact(
            project = environmentForJS.project,
            configuration
        )
    }
}

object JsKlibLoadingPipelinePhase : WebKLibLoadingPipelinePhase(name = "JsKlibLoadingPipelinePhase") {
    override val configFiles: EnvironmentConfigFiles
        get() = EnvironmentConfigFiles.JS_CONFIG_FILES
}

object WasmKlibLoadingPipelinePhase : WebKLibLoadingPipelinePhase(name = "WasmKlibLoadingPipelinePhase") {
    override val configFiles: EnvironmentConfigFiles
        get() = EnvironmentConfigFiles.WASM_CONFIG_FILES
}
