/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "File copy should not be cacheable")
internal abstract class KotlinAbiUpdateTask : DefaultTask() {
    companion object {
        const val SIMPLE_TASK_NAME = "updateAbi"
        const val JVM_TASK_NAME = "updateJvmAbi"
        const val KLIB_TASK_NAME = "updateKlibAbi"
    }

    @get:OutputFile
    abstract val referenceDump: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val actualDump: RegularFileProperty

    @TaskAction
    internal fun overwrite() {
        actualDump.get().asFile.copyTo(referenceDump.get().asFile, overwrite = true)
    }
}
