/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiCheckTask
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiDumpTask

public interface AbiValidationJvmExtension {
    val filters: AbiFiltersSpec

    fun filters(action: Action<AbiFiltersSpec>) {
        action.execute(filters)
    }

    val referenceDumpDir: DirectoryProperty

    val useLegacyFormat: Property<Boolean>

    fun useLegacyFormat() {
        useLegacyFormat.set(true)
    }

    val referenceDumpFileName: Property<String>

    val dumpTaskProvider: TaskProvider<KotlinAbiDumpTask>

    val checkTaskProvider: TaskProvider<KotlinAbiCheckTask>

    val updateTaskProvider: TaskProvider<Task>
}
