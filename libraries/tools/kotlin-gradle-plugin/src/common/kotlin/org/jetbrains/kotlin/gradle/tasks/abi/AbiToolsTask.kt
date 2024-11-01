/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.abi.tools.api.JvmAbiToolsInterface
import org.jetbrains.kotlin.gradle.internal.UsesClassLoadersCachingBuildService
import java.util.*
import kotlin.reflect.KClass

internal abstract class AbiToolsTask : DefaultTask(), UsesClassLoadersCachingBuildService {
    @get:Classpath
    abstract val toolsClasspath: ConfigurableFileCollection

    @TaskAction
    fun execute() {
        val files = toolsClasspath.files.toList()
        val classLoader = classLoadersCachingService.get().getClassLoader(files)
        val tools = loadImplementation(JvmAbiToolsInterface::class, classLoader)
        runTools(tools)
    }

    protected abstract fun runTools(tools: JvmAbiToolsInterface)


    private fun <T : Any> loadImplementation(cls: KClass<T>, classLoader: ClassLoader): T {
        val implementations = ServiceLoader.load(cls.java, classLoader)
        implementations.firstOrNull() ?: error("The classpath contains no implementation for ${cls.qualifiedName}")
        return implementations.singleOrNull()
            ?: error("The classpath contains more than one implementation for ${cls.qualifiedName}")
    }
}
