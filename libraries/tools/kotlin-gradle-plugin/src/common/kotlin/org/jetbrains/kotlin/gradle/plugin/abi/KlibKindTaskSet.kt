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
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiCheckTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiUpdateTask
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinKlibAbiDumpTask

private const val ACTUAL_DUMP_PATH = "abi/klib.abi"

internal fun Project.klibTaskSet(
    extension: AbiValidationMultiplatformExtension,
    toolClasspath: Configuration,
): KlibKindTaskSet {
    val referenceDumpFileName = extension.klib.referenceDumpFileName
    val referenceDump = extension.referenceDumpDir.map { dir -> dir.file(referenceDumpFileName.get()) }
    val useLegacyFormat = extension.useLegacyFormat
    val filters = extension.filters

    return KlibKindTaskSet(
        this,
        referenceDump,
        useLegacyFormat,
        filters,
        toolClasspath,
        KotlinKlibAbiDumpTask.KLIB_TASK_NAME,
        KotlinAbiCheckTaskImpl.KLIB_TASK_NAME,
        KotlinAbiUpdateTask.KLIB_TASK_NAME
    )
}

internal class KlibKindTaskSet(
    private val project: Project,
    referenceDump: Provider<RegularFile>,
    useLegacyFormat: Provider<Boolean>,
    filters: AbiFiltersSpec,
    toolClasspath: Configuration,
    dumpTaskName: String,
    checkTaskName: String,
    updateTaskName: String,
) {
    private val dumpTaskProvider: TaskProvider<KotlinKlibAbiDumpTask>

    init {
        dumpTaskProvider = project.tasks.register(dumpTaskName, KotlinKlibAbiDumpTask::class.java) {
            it.dumpFile.convention(project.layout.buildDirectory.file(ACTUAL_DUMP_PATH))
            it.toolsClasspath.from(toolClasspath)
            it.legacyFormat.convention(useLegacyFormat)
            it.referenceDump.convention(referenceDump)

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

            it.updateTaskName.set(KotlinAbiUpdateTask.SIMPLE_TASK_NAME)
        }

        project.tasks.register(updateTaskName, KotlinAbiUpdateTask::class.java) {
            it.actualDump.convention(dumpTaskProvider.map { t -> t.dumpFile.get() })
            it.referenceDump.convention(referenceDump)
        }
    }

    fun addSuit(suitName: String, targetName: String, klibFile: FileCollection) {
        dumpTaskProvider.configure {
            it.suits.put(suitName, KotlinKlibAbiDumpTask.Suit(targetName, klibFile))
        }
    }

    fun keepSuit(suitName: String, targetName: String) {
        dumpTaskProvider.configure {
            it.unsupportedSuites.put(suitName, targetName)
        }
    }
}