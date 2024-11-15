/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.StepStatus
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineStep
import org.jetbrains.kotlin.cli.pipeline.resultOrFail
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.test.model.FrontendFacade
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.applicationDisposableProvider
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.dependencyProvider

class PhasedJvmFrontedFacade(testServices: TestServices) : FrontendFacade<FirOutputArtifact>(testServices, FrontendKinds.FIR) {
    override fun analyze(module: TestModule): FirOutputArtifact {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val input = ConfigurationPipelineArtifact(
            configuration = configuration,
            diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector),
            rootDisposable = testServices.applicationDisposableProvider.getApplicationRootDisposable(),
        )
        val output = JvmFrontendPipelineStep.execute(input).resultOrFail
        val firOutputs = output.result.outputs


        val modulesFromTheSameStructure = buildList {
            add(module)
            module.dependsOnDependencies.mapTo(this) { dep ->
                testServices.dependencyProvider.getTestModule(dep.moduleName)
            }
        }.associateBy { "<${it.name}>"}
        val testFirOutputs = firOutputs.map {
            val correspondingModule = modulesFromTheSameStructure.getValue(it.session.moduleData.name.asString())
            val testFilePerFirFile = correspondingModule.files.mapNotNull { testFile ->
                val firFile = it.fir.firstOrNull { firFile -> testFile.name == firFile.name } ?: return@mapNotNull null
                testFile to firFile
            }
            FirOutputPartForDependsOnModule(
                module = correspondingModule,
                session = it.session,
                scopeSession = it.scopeSession,
                firAnalyzerFacade = null,
                testFilePerFirFile.toMap()
            )
        }
        return FirOutputArtifactImpl(testFirOutputs, output)
    }
}
