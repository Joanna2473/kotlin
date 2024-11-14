/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.*
import org.jetbrains.kotlin.cli.jvm.compiler.configureSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.ClassicFrontendSpecificJvmConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import java.io.File

object JvmConfigurationPipelinePhase : PipelinePhase<ArgumentsPipelineArtifact<K2JVMCompilerArguments>, ConfigurationPipelineArtifact>(
    name = "JvmConfigurationPipelinePhase",
    step = JvmConfigurationStep,
)

object JvmConfigurationStep : CompilerPipelineStep<ArgumentsPipelineArtifact<K2JVMCompilerArguments>, ConfigurationPipelineArtifact>() {
    override fun execute(input: ArgumentsPipelineArtifact<K2JVMCompilerArguments>): StepStatus<ConfigurationPipelineArtifact> {
        val (arguments, services, rootDisposable, messageCollector) = input
        val configuration = CompilerConfiguration()
        CommonConfigurationFiller<K2JVMCompilerArguments>().fillConfiguration(
            arguments,
            configuration,
            CommonConfigurationFiller.Context(
                messageCollector,
                K2JVMCompiler.K2JVMCompilerPerformanceManager(),
                createMetadataVersion = { versionArray -> MetadataVersion(*versionArray) },
                provideCustomScriptingPluginOptions = this::provideCustomScriptingPluginOptions
            )
        )
        JvmConfigurationFiller.fillConfiguration(arguments, configuration, JvmConfigurationFiller.Context(services))

        if (messageCollector.hasErrors()) return ExitCode.COMPILATION_ERROR.toErrorStatus()
        return ConfigurationPipelineArtifact(configuration, input.diagnosticCollector, rootDisposable).toOkStatus()
    }

    private fun provideCustomScriptingPluginOptions(arguments: K2JVMCompilerArguments): List<String> {
        return buildList {
            if (arguments.scriptTemplates?.isNotEmpty() == true) {
                add("plugin:kotlin.scripting:script-templates=${arguments.scriptTemplates!!.joinToString(",")}")
            }
            if (arguments.scriptResolverEnvironment?.isNotEmpty() == true) {
                add("plugin:kotlin.scripting:script-resolver-environment=${arguments.scriptResolverEnvironment!!.joinToString(",")}")
            }
        }
    }
}

object JvmConfigurationFiller : ConfigurationFiller<K2JVMCompilerArguments, JvmConfigurationFiller.Context>() {
    data class Context(val services: Services)

    override fun fillConfiguration(arguments: K2JVMCompilerArguments, configuration: CompilerConfiguration, context: Context): ExitCode {
        arguments.buildFile?.let { configuration.buildFile = File(it) }
        configuration.allowNoSourceFiles = arguments.allowNoSourceFiles
        configuration.setupJvmSpecificArguments(arguments)
        configuration.setupIncrementalCompilationServices(arguments, context.services)

        val messageCollector = configuration.messageCollector
        configuration.phaseConfig = createPhaseConfig(jvmPhases, arguments, messageCollector)
        if (!configuration.configureJdkHome(arguments)) return ExitCode.COMPILATION_ERROR
        configuration.disableStandardScriptDefinition = arguments.disableStandardScript
        val moduleName = arguments.moduleName ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME
        configuration.moduleName = moduleName
        configuration.configureJavaModulesContentRoots(arguments)
        configuration.configureStandardLibs(configuration.kotlinPaths, arguments)
        configuration.configureAdvancedJvmOptions(arguments)
        configuration.configureKlibPaths(arguments)
        if (arguments.expression == null) {
            configuration.setupModuleChunk(arguments)
        }
        if (arguments.script || arguments.expression != null) {
            configuration.scriptMode = arguments.script
            configuration.freeArgsForScript += arguments.freeArgs
            configuration.expressionToEvaluate = arguments.expression
            configuration.defaultExtensionForScripts = arguments.defaultScriptExtension
        }
        // should be called after configuring jdk home from build file
        configuration.configureJdkClasspathRoots()

        // TODO: consider moving it into separate entity
        if (arguments.useOldBackend) {
            val severity = if (isUseOldBackendAllowed()) CompilerMessageSeverity.WARNING else CompilerMessageSeverity.ERROR
            messageCollector.report(severity, "-Xuse-old-backend is no longer supported. Please migrate to the new JVM IR backend")
            if (severity == CompilerMessageSeverity.ERROR) return ExitCode.COMPILATION_ERROR
        }

        return ExitCode.OK
    }

    private fun CompilerConfiguration.setupIncrementalCompilationServices(arguments: K2JVMCompilerArguments, services: Services) {
        if (!incrementalCompilationIsEnabled(arguments)) return
        lookupTracker = services[LookupTracker::class.java]
        expectActualTracker = services[ExpectActualTracker::class.java]
        inlineConstTracker = services[InlineConstTracker::class.java]
        enumWhenTracker = services[EnumWhenTracker::class.java]
        importTracker = services[ImportTracker::class.java]
        incrementalCompilationComponents = services[IncrementalCompilationComponents::class.java]
        putIfNotNull(ClassicFrontendSpecificJvmConfigurationKeys.JAVA_CLASSES_TRACKER, services[JavaClassesTracker::class.java])
    }

    private fun CompilerConfiguration.setupModuleChunk(arguments: K2JVMCompilerArguments) {
        val buildFile = this.buildFile
        val moduleChunk = configureModuleChunk(arguments, buildFile)
        this.moduleChunk = moduleChunk
        configureSourceRoots(moduleChunk.modules, buildFile)
    }

    private fun isUseOldBackendAllowed(): Boolean {
        return JvmConfigurationFiller::class.java.classLoader.getResource("META-INF/unsafe-allow-use-old-backend") != null
    }
}
