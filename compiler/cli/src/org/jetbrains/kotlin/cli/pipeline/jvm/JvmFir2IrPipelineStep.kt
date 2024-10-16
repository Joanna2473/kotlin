/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializerImpl
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.convertToIrAndActualizeForJvm
import org.jetbrains.kotlin.cli.pipeline.CompilerPipelineStep
import org.jetbrains.kotlin.cli.pipeline.StepStatus
import org.jetbrains.kotlin.cli.pipeline.toOkStatus
import org.jetbrains.kotlin.fir.backend.jvm.JvmFir2IrExtensions

object JvmFir2IrPipelineStep : CompilerPipelineStep<JvmFrontendPipelineArtifact, JvmFir2IrPipelineArtifact>() {
    override fun execute(input: JvmFrontendPipelineArtifact): StepStatus<JvmFir2IrPipelineArtifact> {
        val (firResult, configuration, environment, diagnosticCollector, sourceFiles) = input
        val fir2IrExtensions = JvmFir2IrExtensions(configuration, JvmIrDeserializerImpl())
        val irGenerationExtensions = IrGenerationExtension.Companion.getInstances(environment.project)
        val fir2IrAndIrActualizerResult = firResult.convertToIrAndActualizeForJvm(
            fir2IrExtensions,
            configuration,
            diagnosticCollector,
            irGenerationExtensions
        )

        return JvmFir2IrPipelineArtifact(
            fir2IrAndIrActualizerResult,
            configuration,
            environment,
            diagnosticCollector,
            sourceFiles
        ).toOkStatus()
    }
}
