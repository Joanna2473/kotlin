/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.abi.AbiFiltersSpec
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationJvmExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiUpdateTask
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiCheckTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinJvmAbiDumpTask

private const val ACTUAL_DUMP_PATH = "abi/jvm.abi"

internal fun Project.jvmTaskSet(extension: AbiValidationJvmExtension, toolClasspath: Configuration): JvmKindTaskSet {
    val referenceDumpFileName = extension.referenceDumpFileName
    val referenceDump = extension.referenceDumpDir.map { dir -> dir.file(referenceDumpFileName.get()) }
    val useLegacyFormat = extension.useLegacyFormat
    val filters = extension.filters

    return JvmKindTaskSet(
        this,
        referenceDump,
        useLegacyFormat,
        filters,
        toolClasspath,
        KotlinJvmAbiDumpTask.SIMPLE_TASK_NAME,
        KotlinAbiCheckTaskImpl.SIMPLE_TASK_NAME,
        KotlinAbiUpdateTask.SIMPLE_TASK_NAME
    )
}

internal fun Project.jvmTaskSet(
    extension: AbiValidationMultiplatformExtension,
    toolClasspath: Configuration,
): JvmKindTaskSet {
    val referenceDumpFileName = extension.jvm.referenceDumpFileName
    val referenceDump = extension.referenceDumpDir.map { dir -> dir.file(referenceDumpFileName.get()) }
    val useLegacyFormat = extension.useLegacyFormat
    val filters = extension.filters

    return JvmKindTaskSet(
        this,
        referenceDump,
        useLegacyFormat,
        filters,
        toolClasspath,
        KotlinJvmAbiDumpTask.JVM_TASK_NAME,
        KotlinAbiCheckTaskImpl.JVM_TASK_NAME,
        KotlinAbiUpdateTask.JVM_TASK_NAME
    )
}


internal class JvmKindTaskSet(
    private val project: Project,
    referenceDump: Provider<RegularFile>,
    useLegacyFormat: Provider<Boolean>,
    filters: AbiFiltersSpec,
    toolClasspath: Configuration,
    dumpTaskName: String,
    checkTaskName: String,
    updateTaskName: String,
) {
    private val dumpTaskProvider: TaskProvider<KotlinJvmAbiDumpTask>

    private val suits: MutableMap<String, KotlinJvmAbiDumpTask.SuitClasspath> = mutableMapOf()

    init {
        dumpTaskProvider = project.tasks.register(dumpTaskName, KotlinJvmAbiDumpTask::class.java) {
            it.dumpFile.convention(project.layout.buildDirectory.file(ACTUAL_DUMP_PATH))
            it.toolsClasspath.from(toolClasspath)
            it.legacyFormat.convention(useLegacyFormat)

            it.includedClasses.convention(filters.included.classes)
            it.includedAnnotatedWith.convention(filters.included.annotatedWith)
            it.excludedClasses.convention(filters.excluded.classes)
            it.excludedAnnotatedWith.convention(filters.excluded.annotatedWith)
        }

        project.tasks.register(checkTaskName, KotlinAbiCheckTaskImpl::class.java) {
            it.actualDump.convention(dumpTaskProvider.map { t -> t.dumpFile.get() })
            it.referenceDump.convention(referenceDump)
            it.toolsClasspath.from(toolClasspath)
            it.legacyFormat.convention(useLegacyFormat)

            it.updateTaskName.set(updateTaskName)
        }

        project.tasks.register(updateTaskName, KotlinAbiUpdateTask::class.java) {
            it.actualDump.convention(dumpTaskProvider.map { t -> t.dumpFile.get() })
            it.referenceDump.convention(referenceDump)
        }
    }

    fun addSuit(suitName: String, classFiles: FileCollection) {
        val classpath = suits.computeIfAbsent(suitName) {
            val newClasspath = KotlinJvmAbiDumpTask.SuitClasspath(project.files())
            // at the first mention, we save the suit in the task, then we can add classes because newClasspath is mutable
            dumpTaskProvider.configure { dumpTask ->
                dumpTask.suits.put(suitName, newClasspath)
            }
            newClasspath
        }

        // add classes into new or already saved suit
        classpath.classfilesDirs.from(classFiles)
    }
}

