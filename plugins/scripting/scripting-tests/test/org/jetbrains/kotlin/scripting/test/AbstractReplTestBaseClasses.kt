/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.test.repl.TestReplCompilerPluginRegistrar
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.JvmBinaryArtifactHandler
import org.jetbrains.kotlin.test.backend.handlers.computeTestRuntimeClasspath
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.PREFER_IN_TEST_OVER_STDLIB
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.FirReplFrontendFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirScriptAndReplCodegenTest
import org.jetbrains.kotlin.test.runners.enableLazyResolvePhaseChecking
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator.Companion.TEST_CONFIGURATION_KIND_KEY
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import java.io.ByteArrayOutputStream
import java.io.PrintStream

open class AbstractReplWithTestExtensionsDiagnosticsTest : AbstractKotlinCompilerTest() {
    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration(frontendFacade = ::FirReplFrontendFacade)
        enableLazyResolvePhaseChecking()
        configureFirParser(FirParser.Psi)
        useConfigurators(
            ::ReplConfigurator
        )
        defaultDirectives {
            +WITH_STDLIB
        }
    }
}

open class AbstractReplWithTestExtensionsCodegenTest : AbstractFirScriptAndReplCodegenTest(::FirReplFrontendFacade) {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useConfigurators(
                ::ReplConfigurator
            )
            defaultDirectives {
                +WITH_STDLIB
            }
            jvmArtifactsHandlersStep {
                useHandlers(
                    ::ReplRunChecker
                )
            }
        }
    }
}

@OptIn(ExperimentalCompilerApi::class)
private class ReplConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, TestReplCompilerPluginRegistrar())
    }
}

private class ReplRunChecker(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {

    private var scriptProcessed = false
    private val replState: MutableMap<String, Any?> = mutableMapOf()
    private var currentReplClassloader: GeneratedClassLoader? = null
    private val classLoadersToDispose: MutableList<GeneratedClassLoader> = mutableListOf()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        val fileInfos = info.fileInfos.ifEmpty { return }
        currentReplClassloader = generatedReplSnippetTestClassLoader(testServices, module, info.classFileFactory, currentReplClassloader)
        for (fileInfo in fileInfos) {
            when (val sourceFile = fileInfo.sourceFile) {
                is KtPsiSourceFile -> (sourceFile.psiFile as? KtFile)?.let { ktFile ->
                    ktFile.script?.fqName?.let { scriptFqName ->
                        runAndCheckSnippet(ktFile, scriptFqName, currentReplClassloader!!)
                        scriptProcessed = true
                    }
                }
                else -> {
                    assertions.fail { "Only PSI scripts are supported so far" }
                }
            }
        }
    }

    private fun generatedReplSnippetTestClassLoader(
        testServices: TestServices,
        module: TestModule,
        classFileFactory: ClassFileFactory,
        previousSnippetClassLoader: GeneratedClassLoader?,
    ): GeneratedClassLoader {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val classpath = computeTestRuntimeClasspath(testServices, module)
        var parentClassLoader: ClassLoader? = previousSnippetClassLoader
        if (PREFER_IN_TEST_OVER_STDLIB in module.directives) {
            val libPathProvider = testServices.standardLibrariesPathProvider
            classpath += libPathProvider.runtimeJarForTests()
            if (configuration[TEST_CONFIGURATION_KIND_KEY]?.withReflection == true) {
                classpath += libPathProvider.reflectJarForTests()
            }
            classpath += libPathProvider.scriptRuntimeJarForTests()
            classpath += libPathProvider.kotlinTestJarForTests()
        } else if (parentClassLoader == null) {
            parentClassLoader = if (configuration[TEST_CONFIGURATION_KIND_KEY]?.withReflection == true) {
                testServices.standardLibrariesPathProvider.getRuntimeAndReflectJarClassLoader()
            } else {
                testServices.standardLibrariesPathProvider.getRuntimeJarClassLoader()
            }
        }
        return GeneratedClassLoader(classFileFactory, parentClassLoader, *classpath.map { it.toURI().toURL() }.toTypedArray()).also {
            classLoadersToDispose.add(it)
        }
    }

    private fun runAndCheckSnippet(
        ktFile: KtFile,
        scriptFqName: FqName,
        classLoader: GeneratedClassLoader,
    ) {
        val expected = Regex("// expected out: (.*)").findAll(ktFile.text).map {
            it.groups[1]!!.value
        }.joinToString("\n")

        val scriptClass = classLoader.loadClass(scriptFqName.asString())
        val ctor = scriptClass.constructors.single()
        val eval = scriptClass.methods.find { it.name.contains("eval") }!!

        val res = captureOut {
            val snippet = ctor.newInstance(replState)
            eval.invoke(snippet)
        }

        assertions.assertEquals(expected, res)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        classLoadersToDispose.forEach { it.dispose() }
        if (!scriptProcessed) {
            assertions.fail { "Can't find script to test" }
        }
    }
}

private fun captureOut(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString()
}
