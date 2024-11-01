/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinJvmAbiDumpTask

internal suspend fun Project.abiValidationForKotlinJvm(abiClasspath: Configuration) {
    val extension = kotlinJvmExtension
    val taskSet = taskSetForKotlinJvmOrAndroid(extension.abiValidation, abiClasspath)

    // wait until all compilations are configured
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    extension.target.compilations.all { compilation ->
        // TODO additional source sets!
        if (compilation.name == SourceSet.MAIN_SOURCE_SET_NAME) {
            taskSet.addSuit(KotlinJvmAbiDumpTask.KOTLIN_JVM_SUIT_NAME, compilation.output.classesDirs)
        }
    }

}