/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.abi

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.abi.AbiFiltersSpec
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationJvmKindExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.abi.*
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiCheckTask
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiUpdateTask
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinJvmAbiDumpTask
import org.jetbrains.kotlin.gradle.tasks.locateTask
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal abstract class AbiValidationMultiplatformExtensionImpl @Inject constructor(private val project: Project) :
    AbiValidationMultiplatformExtension {
    override val filters: AbiFiltersSpec = project.objects.newInstance<AbiFiltersSpecImpl>(project.objects)

    override val jvm: AbiValidationJvmKindExtension = project.objects.newInstance<AbiValidationJvmKindExtensionImpl>(project)

    override val updateTaskProvider: TaskProvider<Task>
        get() = project.locateTask(KotlinAbiUpdateTask.SIMPLE_TASK_NAME)
            ?: throw GradleException("Couldn't locate task ${KotlinAbiUpdateTask.SIMPLE_TASK_NAME}")

    override val dumpTaskProvider: TaskProvider<Task>
        get() = project.locateTask(KotlinJvmAbiDumpTask.SIMPLE_TASK_NAME)
            ?: throw GradleException("Couldn't locate task ${KotlinJvmAbiDumpTask.SIMPLE_TASK_NAME}")

    override val checkTaskProvider: TaskProvider<Task>
        get() = project.locateTask(KotlinAbiCheckTaskImpl.SIMPLE_TASK_NAME)
            ?: throw GradleException("Couldn't locate task ${KotlinAbiCheckTaskImpl.SIMPLE_TASK_NAME}")
}

internal abstract class AbiValidationJvmKindExtensionImpl @Inject constructor(private val project: Project) :
    AbiValidationJvmKindExtension {
    override val updateTaskProvider: TaskProvider<Task>
        get() = project.locateTask(KotlinAbiUpdateTask.JVM_TASK_NAME)
            ?: throw GradleException("Couldn't locate task ${KotlinAbiUpdateTask.SIMPLE_TASK_NAME}")

    override val dumpTaskProvider: TaskProvider<KotlinAbiDumpTask>
        get() = project.locateTask(KotlinJvmAbiDumpTask.JVM_TASK_NAME)
            ?: throw GradleException("Couldn't locate task ${KotlinJvmAbiDumpTask.SIMPLE_TASK_NAME}")

    override val checkTaskProvider: TaskProvider<KotlinAbiCheckTask>
        get() = project.locateTask(KotlinAbiCheckTaskImpl.JVM_TASK_NAME)
            ?: throw GradleException("Couldn't locate task ${KotlinAbiCheckTaskImpl.SIMPLE_TASK_NAME}")
}
