/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface
import org.jetbrains.kotlin.abi.tools.api.JvmAbiSuit

internal abstract class KotlinJvmAbiDumpTask : AbiToolsTask(), KotlinAbiDumpTask {
    companion object {
        const val SIMPLE_TASK_NAME = "dumpAbi"
        const val JVM_TASK_NAME = "dumpJvmAbi"
        const val KOTLIN_JVM_SUIT_NAME = "jvm"
    }

    @get:OutputFile
    abstract override val dumpFile: RegularFileProperty

    @get:Nested
    abstract val suits: MapProperty<String, SuitClasspath>

    @get:Input
    abstract val legacyFormat: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val includedClasses: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val excludedClasses: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val includedAnnotatedWith: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val excludedAnnotatedWith: SetProperty<String>

    override fun runTools(tools: AbiToolsInterface) {
        val jvmSuits = suits.get()
            .mapValues { classpath ->
                classpath.value.classfilesDirs.asFileTree
                    .asSequence()
                    .filter {
                        !it.isDirectory && it.name.endsWith(".class") && !it.name.startsWith("META-INF/")
                    }
            }
            .map { entry ->
                JvmAbiSuit(entry.key, entry.value.asIterable())
            }

        val filters = AbiFilters(
            includedClasses.getOrElse(emptySet()),
            excludedClasses.getOrElse(emptySet()),
            includedAnnotatedWith.getOrElse(emptySet()),
            excludedAnnotatedWith.getOrElse(emptySet())
        )
        tools.jvm.dumpTo(dumpFile.get().asFile, jvmSuits, filters)
    }

    internal class SuitClasspath(
        @get:InputFiles
        @get:Optional
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val classfilesDirs: ConfigurableFileCollection,
    )
}
