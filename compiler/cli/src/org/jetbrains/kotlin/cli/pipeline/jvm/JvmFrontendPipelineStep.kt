/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.createLibraryListForJvm
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createContextForIncrementalCompilation
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createIncrementalCompilationScope
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.createProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.targetDescription
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource

object JvmFrontendPipelineStep : CompilerPipelineStep<ConfigurationPipelineArtifact, JvmFrontendPipelineArtifact>() {
    override fun execute(input: ConfigurationPipelineArtifact): StepStatus<JvmFrontendPipelineArtifact> {
        val (configuration, rootDisposable) = input
        val messageCollector = configuration.messageCollector

        if (!FirKotlinToJvmBytecodeCompiler.checkNotSupportedPlugins(configuration, messageCollector)) {
            return ExitCode.COMPILATION_ERROR.toErrorStatus()
        }

        val chunk = configuration.moduleChunk!!
        val targetDescription = chunk.targetDescription()
        val (environment, sources) = createEnvironmentAndSources(
            configuration,
            rootDisposable,
            targetDescription
        ) ?: return ExitCode.COMPILATION_ERROR.toErrorStatus()

        val performanceManager = configuration.perfManager
        performanceManager?.notifyAnalysisStarted()

        val allSources = sources.allFiles

        if (
            allSources.isEmpty() &&
            !configuration.allowNoSourceFiles &&
            configuration.buildFile == null
        ) {
            return when (configuration.version) {
                true -> ExitCode.OK.toErrorStatus()
                false -> {
                    messageCollector.report(CompilerMessageSeverity.ERROR, "No source files")
                    ExitCode.COMPILATION_ERROR.toErrorStatus()
                }
            }
        }

        val sourceScope: AbstractProjectFileSearchScope
        when (configuration.useLightTree) {
            true -> {
                sourceScope = AbstractProjectFileSearchScope.EMPTY
            }
            false -> {
                val ktFiles = allSources.map { (it as KtPsiSourceFile).psiFile }
                sourceScope = environment.getSearchScopeByPsiFiles(ktFiles) + environment.getSearchScopeForProjectJavaSources()
            }
        }

        var librariesScope = environment.getSearchScopeForProjectLibraries()
        val incrementalCompilationScope = createIncrementalCompilationScope(
            configuration,
            environment,
            incrementalExcludesScope = sourceScope
        )?.also { librariesScope -= it }

        val moduleName = when {
            chunk.modules.size > 1 -> chunk.modules.joinToString(separator = "+") { it.getModuleName() }
            else -> configuration.moduleName!!
        }

        val libraryList = createLibraryListForJvm(
            moduleName,
            configuration,
            friendPaths = chunk.modules.fold(emptyList()) { paths, m -> paths + m.getFriendPaths() }
        )

        val sessionsWithSources = prepareJvmSessions<KtSourceFile>(
            files = allSources,
            rootModuleName = Name.special("<$moduleName>"),
            configuration = configuration,
            projectEnvironment = environment,
            librariesScope = librariesScope,
            libraryList = libraryList,
            isCommonSource = sources.isCommonSourceForLt,
            isScript = { false },
            fileBelongsToModule = sources.fileBelongsToModuleForLt,
            createProviderAndScopeForIncrementalCompilation = { files ->
                val scope = environment.getSearchScopeBySourceFiles(files)
                createContextForIncrementalCompilation(
                    configuration,
                    environment,
                    scope,
                    previousStepsSymbolProviders = emptyList(),
                    incrementalCompilationScope
                )
            }
        )

        val countFilesAndLines = if (performanceManager == null) null else performanceManager::addSourcesStats
        val diagnosticsCollector = DiagnosticReporterFactory.createPendingReporter(messageCollector)
        val outputs = sessionsWithSources.map { (session, sources) ->
            val rawFirFiles = when (configuration.useLightTree) {
                true -> session.buildFirViaLightTree(sources, diagnosticsCollector, countFilesAndLines)
                else -> session.buildFirFromKtFiles(sources.asKtFilesList())
            }
            resolveAndCheckFir(session, rawFirFiles, diagnosticsCollector)
        }
        outputs.runPlatformCheckers(diagnosticsCollector)
        performanceManager?.notifyAnalysisFinished()

        if (diagnosticsCollector.hasErrors) {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
                diagnosticsCollector, messageCollector,
                configuration.renderDiagnosticInternalName
            )
            return ExitCode.COMPILATION_ERROR.toErrorStatus()
        }
        val kotlinPackageUsageIsFine = when (configuration.useLightTree) {
            true -> outputs.all { checkKotlinPackageUsageForLightTree(configuration, it.fir) }
            false -> sessionsWithSources.all { (_, sources) -> checkKotlinPackageUsageForPsi(configuration, sources.asKtFilesList()) }
        }

        if (!kotlinPackageUsageIsFine) return ExitCode.COMPILATION_ERROR.toErrorStatus()

        val firResult = FirResult(outputs)
        return JvmFrontendPipelineArtifact(firResult, configuration, environment, diagnosticsCollector, allSources).toOkStatus()
    }

    private data class EnvironmentAndSources(val environment: VfsBasedProjectEnvironment, val sources: GroupedKtSources)

    private fun createEnvironmentAndSources(
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        targetDescription: String
    ): EnvironmentAndSources? {
        val messageCollector = configuration.messageCollector
        return when (configuration.useLightTree) {
            true -> {
                val environment = createProjectEnvironment(
                    configuration,
                    rootDisposable,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES,
                    messageCollector
                )
                val sources = collectSources(configuration, environment.project, messageCollector)
                EnvironmentAndSources(environment, sources)
            }
            false -> {
                val kotlinCoreEnvironment = K2JVMCompiler.Companion.createCoreEnvironment(
                    rootDisposable, configuration, messageCollector,
                    targetDescription
                ) ?: return null

                val projectEnvironment = VfsBasedProjectEnvironment(
                    kotlinCoreEnvironment.project,
                    VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
                ) { kotlinCoreEnvironment.createPackagePartProvider(it) }

                EnvironmentAndSources(projectEnvironment, groupKtFiles(kotlinCoreEnvironment.getSourceFiles()))
            }
        }.takeUnless { messageCollector.hasErrors() }
    }

    private fun groupKtFiles(ktFiles: List<KtFile>): GroupedKtSources {
        val platformSources = mutableSetOf<KtPsiSourceFile>()
        val commonSources = mutableSetOf<KtPsiSourceFile>()
        val sourcesByModuleName = mutableMapOf<String, MutableSet<KtPsiSourceFile>>()

        for (ktFile in ktFiles) {
            val sourceFile = KtPsiSourceFile(ktFile)
            if (ktFile.isCommonSource == true) {
                commonSources.add(sourceFile)
                continue
            }
            when (val moduleName = ktFile.hmppModuleName) {
                null -> platformSources.add(sourceFile)
                else -> {
                    commonSources.add(sourceFile)
                    sourcesByModuleName.getOrPut(moduleName) { mutableSetOf() }.add(sourceFile)
                }
            }
        }
        return GroupedKtSources(platformSources, commonSources, sourcesByModuleName)
    }
}
