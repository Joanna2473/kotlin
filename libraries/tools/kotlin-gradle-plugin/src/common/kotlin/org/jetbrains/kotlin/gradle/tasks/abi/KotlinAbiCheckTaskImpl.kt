/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.abi.tools.api.JvmAbiToolsInterface

@DisableCachingByDefault(because = "No output")
internal abstract class KotlinAbiCheckTaskImpl : AbiToolsTask(), KotlinAbiCheckTask {
    companion object {
        const val SIMPLE_TASK_NAME = "checkAbi"
        const val JVM_TASK_NAME = "checkJvmAbi"
        const val KLIB_TASK_NAME = "checkKlibAbi"
    }

    @get:InputFiles // don't fail the task if file does not exist, instead print custom error message from verify()
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract override val referenceDump: RegularFileProperty

    @get:InputFiles // don't fail the task if file does not exist, instead print custom error message from verify()
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract override val actualDump: RegularFileProperty

    @get:Input
    abstract val updateTaskName: Property<String>

    @get:Input
    abstract val legacyFormat: Property<Boolean>

    private val projectName = project.name

    private val rootDir = project.rootDir


    override fun runTools(tools: JvmAbiToolsInterface) {
        val referenceDumpFile = referenceDump.get().asFile
        val actualDumpFile = actualDump.get().asFile

        if (!referenceDumpFile.exists()) {
            error(
                "Expected file with ABI declarations '${referenceDumpFile.relativeTo(rootDir)}' does not exist.\n" +
                        "Please ensure that ':apiDump' was executed in order to get " +
                        "an API dump to compare the build against"
            )
        }
        if (!actualDumpFile.exists()) {
            error(
                "Expected file with generated ABI declarations '${actualDumpFile.relativeTo(rootDir)}'" +
                        " does not exist."
            )
        }

        val diffSet = mutableSetOf<String>()
        val diff = tools.filesDiff(
            referenceDumpFile,
            actualDumpFile
        )
        if (diff != null) diffSet.add(diff)
        if (diffSet.isNotEmpty()) {
            val diffText = diffSet.joinToString("\n\n")
            error(
                "ABI check failed for project $projectName.\n$diffText\n\n" +
                        "You can run :$projectName:${updateTaskName.get()} task to overwrite ABI declarations"
            )
        }
    }
}
