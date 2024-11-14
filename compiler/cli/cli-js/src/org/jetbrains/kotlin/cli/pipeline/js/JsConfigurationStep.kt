/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.js

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.js.DisposableZipFileSystemAccessor
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.js.dtsStrategy
import org.jetbrains.kotlin.cli.js.granularity
import org.jetbrains.kotlin.cli.js.targetVersion
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File
import java.io.IOException

object JsConfigurationStep : CompilerPipelineStep<
        ArgumentsPipelineArtifact<K2JSCompilerArguments>,
        ConfigurationPipelineArtifact
        >() {

    override fun execute(input: ArgumentsPipelineArtifact<K2JSCompilerArguments>): StepStatus<ConfigurationPipelineArtifact> {
        val (arguments, services, rootDisposable, messageCollector, performanceManager) = input
        val configuration = CompilerConfiguration()
        CommonConfigurationFiller<K2JSCompilerArguments>().fillConfiguration(
            arguments,
            configuration,
            CommonConfigurationFiller.Context(
                messageCollector,
                performanceManager,
                createMetadataVersion = { versionArray -> KlibMetadataVersion(*versionArray) },
                provideCustomScriptingPluginOptions = null
            )
        )
        JsConfigurationFiller.fillConfiguration(arguments, configuration, JsConfigurationFiller.Context(rootDisposable, services))

        if (messageCollector.hasErrors()) return COMPILATION_ERROR.toErrorStatus()
        return ConfigurationPipelineArtifact(configuration, input.diagnosticCollector, rootDisposable).toOkStatus()
    }
}

object JsConfigurationFiller : ConfigurationFiller<K2JSCompilerArguments, JsConfigurationFiller.Context>() {
    class Context(val rootDisposable: Disposable, val services: Services)

    override fun fillConfiguration(arguments: K2JSCompilerArguments, configuration: CompilerConfiguration, context: Context): ExitCode {
        return configuration.fillConfigurationImpl(arguments, context)
    }

    private fun CompilerConfiguration.fillConfigurationImpl(arguments: K2JSCompilerArguments, context: Context): ExitCode {
        val messageCollector = messageCollector
        val services = context.services

        // -------------- from setupPlatformSpecificArgumentsAndServices --------------
        setupCommonKlibArguments(arguments)

        @Suppress("DEPRECATION")
        if (arguments.outputFile != null) {
            messageCollector.report(WARNING, "The '-output' command line option does nothing and will be removed in a future release")
        }
        @Suppress("DEPRECATION")
        if (arguments.noStdlib) {
            messageCollector.report(WARNING, "The '-no-stdlib' command line option does nothing and will be removed in a future release")
        }
        @Suppress("DEPRECATION")
        if (arguments.metaInfo) {
            messageCollector.report(WARNING, "The '-meta-info' command line option does nothing and will be removed in a future release")
        }
        @Suppress("DEPRECATION")
        if (arguments.typedArrays) {
            messageCollector.report(
                WARNING,
                "The '-Xtyped-arrays' command line option does nothing and will be removed in a future release"
            )
        }

        if (arguments.debuggerCustomFormatters) {
            useDebuggerCustomFormatters = true
        }

        if (arguments.sourceMap) {
            sourceMap = true
            if (arguments.sourceMapPrefix != null) {
                sourceMapPrefix = arguments.sourceMapPrefix!!
            }

            var sourceMapSourceRoots = arguments.sourceMapBaseDirs
            if (sourceMapSourceRoots == null && StringUtil.isNotEmpty(arguments.sourceMapPrefix)) {
                sourceMapSourceRoots = K2JSCompiler.calculateSourceMapSourceRoot(messageCollector, arguments)
            }

            if (sourceMapSourceRoots != null) {
                val sourceMapSourceRootList = StringUtil.split(sourceMapSourceRoots, File.pathSeparator)
                put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceMapSourceRootList)
            }

        } else {
            if (arguments.sourceMapPrefix != null) {
                messageCollector.report(WARNING, "source-map-prefix argument has no effect without source map", null)
            }
            if (arguments.sourceMapBaseDirs != null) {
                messageCollector.report(WARNING, "source-map-source-root argument has no effect without source map", null)
            }
        }

        friendPathsDisabled = arguments.friendModulesDisabled
        generateStrictImplicitExport = arguments.strictImplicitExportType

        // -------------- from doExecute --------------
        val friendModules = arguments.friendModules
        if (!arguments.friendModulesDisabled && friendModules != null) {
            val friendPaths = friendModules
                .split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .filterNot { it.isEmpty() }

            addAll(JSConfigurationKeys.FRIEND_PATHS, friendPaths)
        }

        if (arguments.wasm) {
            // K/Wasm support ES modules only.
            moduleKind = ModuleKind.ES
        }

        incrementalDataProvider = services[IncrementalDataProvider::class.java]
        incrementalResultsConsumer = services[IncrementalResultsConsumer::class.java]
        incrementalNextRoundChecker = services[IncrementalNextRoundChecker::class.java]
        lookupTracker = services[LookupTracker::class.java]
        expectActualTracker = services[ExpectActualTracker::class.java]

        val sourceMapEmbedContentString = arguments.sourceMapEmbedSources
        var sourceMapContentEmbedding: SourceMapSourceEmbedding? = if (sourceMapEmbedContentString != null) {
            K2JSCompiler.sourceMapContentEmbeddingMap[sourceMapEmbedContentString]
        } else {
            SourceMapSourceEmbedding.INLINING
        }
        if (sourceMapContentEmbedding == null) {
            messageCollector.report(
                ERROR,
                "Unknown source map source embedding mode: $sourceMapEmbedContentString. Valid values are: ${K2JSCompiler.sourceMapContentEmbeddingMap.keys.joinToString()}"
            )
            sourceMapContentEmbedding = SourceMapSourceEmbedding.INLINING
        }
        sourceMapEmbedSources = sourceMapContentEmbedding
        sourceMapIncludeMappingsFromUnavailableFiles = arguments.includeUnavailableSourcesIntoSourceMap

        if (!arguments.sourceMap && sourceMapEmbedContentString != null) {
            messageCollector.report(WARNING, "source-map-embed-sources argument has no effect without source map", null)
        }

        val sourceMapNamesPolicyString = arguments.sourceMapNamesPolicy
        var sourceMapNamesPolicy: SourceMapNamesPolicy? = if (sourceMapNamesPolicyString != null) {
            K2JSCompiler.sourceMapNamesPolicyMap[sourceMapNamesPolicyString]
        } else {
            SourceMapNamesPolicy.SIMPLE_NAMES
        }
        if (sourceMapNamesPolicy == null) {
            messageCollector.report(
                ERROR,
                "Unknown source map names policy: $sourceMapNamesPolicyString. Valid values are: ${K2JSCompiler.sourceMapNamesPolicyMap.keys.joinToString()}"
            )
            sourceMapNamesPolicy = SourceMapNamesPolicy.SIMPLE_NAMES
        }
        this.sourcemapNamesPolicy = sourceMapNamesPolicy

        printReachabilityInfo = arguments.irDcePrintReachabilityInfo
        fakeOverrideValidator = arguments.fakeOverrideValidator
        arguments.irDceDumpReachabilityInfoToFile?.let { dumpReachabilityInfoToFile = it }

        setupPartialLinkageConfig(
            mode = arguments.partialLinkageMode,
            logLevel = arguments.partialLinkageLogLevel,
            compilerModeAllowsUsingPartialLinkage =
                /* no PL when producing KLIB */ arguments.includes != null,
            onWarning = { messageCollector.report(WARNING, it) },
            onError = { messageCollector.report(ERROR, it) }
        )

        // -------------- unique --------------

        wasmCompilation = arguments.wasm
        arguments.includes?.let { includes = it }
        produceKlibFile = arguments.irProduceKlibFile
        produceKlibDir = arguments.irProduceKlibDir
        granularity = arguments.granularity
        tsCompilationStrategy = arguments.dtsStrategy
        callMain = arguments.main != K2JsArgumentConstants.NO_CALL

        // -------------- from doExecute --------------

        val targetVersion = arguments.targetVersion?.also {
            target = it
        }

        if (targetVersion == null) {
            messageCollector.report(ERROR, "Unsupported ECMA version: ${arguments.target}")
            return COMPILATION_ERROR
        }

        if (arguments.script) {
            messageCollector.report(ERROR, "K/JS does not support Kotlin script (*.kts) files")
            return COMPILATION_ERROR
        }

        if (arguments.freeArgs.isEmpty() && !(incrementalCompilationIsEnabledForJs(arguments))) {
            if (arguments.version) {
                return OK
            }
            if (arguments.includes.isNullOrEmpty()) {
                messageCollector.report(ERROR, "Specify at least one source file or directory", null)
                return COMPILATION_ERROR
            }
        }

        val libraries: List<String> = K2JSCompiler.configureLibraries(arguments.libraries) + listOfNotNull(arguments.includes)
        val friendLibraries: List<String> = K2JSCompiler.configureLibraries(arguments.friendModules)

        this.libraries += libraries
        this.friendLibraries += friendLibraries
        this.transitiveLibraries += libraries

        put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, arguments.wasmEnableArrayRangeChecks)
        put(WasmConfigurationKeys.WASM_ENABLE_ASSERTS, arguments.wasmEnableAsserts)
        put(WasmConfigurationKeys.WASM_GENERATE_WAT, arguments.wasmGenerateWat)
        put(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS, arguments.wasmUseTrapsInsteadOfExceptions)
        put(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL, arguments.wasmUseNewExceptionProposal)
        put(WasmConfigurationKeys.WASM_USE_JS_TAG, arguments.wasmUseJsTag ?: arguments.wasmUseNewExceptionProposal)
        putIfNotNull(WasmConfigurationKeys.WASM_TARGET, arguments.wasmTarget?.let(WasmTarget::fromName))

        optimizeGeneratedJs = arguments.optimizeGeneratedJs

        val commonSourcesArray = arguments.commonSources
        val commonSources = commonSourcesArray?.toSet() ?: emptySet()
        val hmppCliModuleStructure = hmppModuleStructure
        for (arg in arguments.freeArgs) {
            addKotlinSourceRoot(arg, commonSources.contains(arg), hmppCliModuleStructure?.getModuleNameForSource(arg))
        }

        arguments.relativePathBases?.let {
            klibRelativePathBases += it
        }

        klibNormalizeAbsolutePath = arguments.normalizeAbsolutePath
        produceKlibSignaturesClashChecks = arguments.enableSignatureClashChecks

        noDoubleInlining = arguments.noDoubleInlining
        duplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.parseOrDefault(
            arguments.duplicatedUniqueNameStrategy,
            default = DuplicatedUniqueNameStrategy.DENY
        )

        val isES2015 = targetVersion == EcmaVersion.es2015
        val moduleKind = this.moduleKind
            ?: K2JSCompiler.moduleKindMap[arguments.moduleKind]
            ?: ModuleKind.ES.takeIf { isES2015 }
            ?: ModuleKind.UMD

        this.moduleKind = moduleKind
        allowKotlinPackage = arguments.allowKotlinPackage
        renderDiagnosticInternalName = arguments.renderInternalDiagnosticNames
        propertyLazyInitialization = arguments.irPropertyLazyInitialization
        generatePolyfills = arguments.generatePolyfills
        generateDts = arguments.generateDts
        generateInlineAnonymousFunctions = arguments.irGenerateInlineAnonymousFunctions
        useEs6Classes = arguments.useEsClasses ?: isES2015
        compileSuspendAsJsGenerator = arguments.useEsGenerators ?: isES2015
        compileLambdasAsEs6ArrowFunctions = arguments.useEsArrowFunctions ?: isES2015


        arguments.platformArgumentsProviderJsExpression?.let {
            definePlatformMainFunctionArguments = it
        }

        val zipAccessor = DisposableZipFileSystemAccessor(64)
        Disposer.register(context.rootDisposable, zipAccessor)
        zipFileSystemAccessor = zipAccessor

        val outputDirPath = arguments.outputDir
        val outputName = arguments.moduleName
        if (outputDirPath == null) {
            messageCollector.report(ERROR, "IR: Specify output dir via -ir-output-dir", location = null)
            return COMPILATION_ERROR
        }

        if (outputName == null) {
            messageCollector.report(ERROR, "IR: Specify output name via -ir-output-name", location = null)
            return COMPILATION_ERROR
        }

        if (messageCollector.hasErrors()) {
            return COMPILATION_ERROR
        }

//        if (arguments.verbose) {
//            reportCompiledSourcesList(messageCollector, sourcesFiles)
//        }

        val moduleName = arguments.irModuleName ?: outputName
        put(CommonConfigurationKeys.MODULE_NAME, moduleName)
        this.outputName = moduleName

        try {
            outputDir = File(outputDirPath).canonicalFile
        } catch (_: IOException) {
            messageCollector.report(ERROR, "Could not resolve output directory", location = null)
            return COMPILATION_ERROR
        }

        perModuleOutputName = arguments.irPerModuleOutputName

        return OK
    }

    /*
        private fun reportCompiledSourcesList(messageCollector: MessageCollector, sourceFiles: List<KtFile>) {
            val fileNames = sourceFiles.map { file ->
                val virtualFile = file.virtualFile
                if (virtualFile != null) {
                    MessageUtil.virtualFileToPath(virtualFile)
                } else {
                    file.name + " (no virtual file)"
                }
            }
            messageCollector.report(LOGGING, "Compiling source files: " + join(fileNames, ", "), null)
        }
     */
}
