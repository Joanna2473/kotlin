/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await

internal suspend fun Project.abiValidationForKotlinAndroid(abiClasspath: Configuration) {
    val extension = kotlinExtension as KotlinAndroidProjectExtension
    val taskSet = jvmTaskSet(extension.abiValidation, abiClasspath)

    // wait until all compilations are configured
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    extension.target.compilations.all { compilation ->
        if (!compilation.androidVariant.isTestVariant) {
            taskSet.addSuit(compilation.androidVariant.name, compilation.output.classesDirs)
        }
    }
}