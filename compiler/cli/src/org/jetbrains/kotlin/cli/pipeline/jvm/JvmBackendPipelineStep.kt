/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.buildFile
import org.jetbrains.kotlin.cli.common.moduleChunk
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.codegenFactoryWithJvmIrBackendInput
import org.jetbrains.kotlin.cli.pipeline.CompilerPipelineStep
import org.jetbrains.kotlin.cli.pipeline.StepStatus
import org.jetbrains.kotlin.cli.pipeline.toErrorStatus
import org.jetbrains.kotlin.cli.pipeline.toOkStatus
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendClassResolver
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmBackendExtension
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus

object JvmBackendPipelineStep : CompilerPipelineStep<JvmFir2IrPipelineArtifact, JvmBinaryPipelineArtifact>() {
    override fun execute(input: JvmFir2IrPipelineArtifact): StepStatus<JvmBinaryPipelineArtifact> {
        val (fir2IrResult, configuration, environment, diagnosticCollector, allSourceFiles) = input

        val moduleDescriptor = fir2IrResult.irModuleFragment.descriptor
        val project = environment.project
        val bindingContext = NoScopeRecordCliBindingTrace(project).bindingContext
        val classResolver = FirJvmBackendClassResolver(fir2IrResult.components)
        val jvmBackendExtension = FirJvmBackendExtension(
            fir2IrResult.components,
            fir2IrResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
        )
        val (codegenFactory, baseBackendInput) = fir2IrResult.codegenFactoryWithJvmIrBackendInput(configuration, jvmBackendExtension)

        val chunk = configuration.moduleChunk!!.modules
        val localFileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
        val codegenInputs = ArrayList<CodegenFactory.CodegenInput>(chunk.size)

        val buildFile = configuration.buildFile
        for (module in chunk) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            val moduleConfiguration = configuration.applyModuleProperties(module, buildFile)
            val backendInput = when (configuration.useLightTree) {
                true -> when (chunk.size) {
                    1 -> baseBackendInput
                    else -> {
                        val wholeModule = baseBackendInput.irModuleFragment
                        val moduleCopy = IrModuleFragmentImpl(wholeModule.descriptor)
                        wholeModule.files.filterTo(moduleCopy.files) { file ->
                            file.fileEntry.name in module.getSourceFiles()
                        }
                        baseBackendInput.copy(irModuleFragment = moduleCopy)
                    }
                }

                false -> {
                    val sourceFiles = module.getSourceFiles(
                        allSourceFiles.asKtFilesList(), localFileSystem,
                        multiModuleChunk = chunk.size > 1, buildFile
                    )
                    codegenFactory.getModuleChunkBackendInput(baseBackendInput, sourceFiles)
                }
            }

            codegenInputs += KotlinToJVMBytecodeCompiler.runLowerings(
                project, moduleConfiguration, moduleDescriptor, bindingContext,
                sourceFiles = null, module, codegenFactory, backendInput, diagnosticCollector,
                classResolver
            )
        }

        val outputs = ArrayList<GenerationState>(chunk.size)

        for (input in codegenInputs) {
            // Codegen (per module)
            outputs += KotlinToJVMBytecodeCompiler.runCodegen(
                input,
                input.state,
                codegenFactory,
                diagnosticCollector,
                configuration
            )
        }

        val success = writeOutputsIfNeeded(project, configuration, configuration.messageCollector, outputs, mainClassFqName = null)

        if (!success) return ExitCode.COMPILATION_ERROR.toErrorStatus()

        return JvmBinaryPipelineArtifact(outputs).toOkStatus()
    }
}
