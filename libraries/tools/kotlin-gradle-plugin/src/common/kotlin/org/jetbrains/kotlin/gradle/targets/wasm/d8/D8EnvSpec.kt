/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.d8

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import org.jetbrains.kotlin.gradle.utils.getFile

/**
 * Spec for D8 - this target is available only for Wasm
 */
@ExperimentalWasmDsl
abstract class D8EnvSpec : org.jetbrains.kotlin.gradle.targets.js.d8.D8EnvSpec() {

    val Project.d8SetupTaskProvider: TaskProvider<out D8SetupTask>
        get() = project.tasks.withType(D8SetupTask::class.java).named(D8SetupTask.NAME)

    companion object {
        const val EXTENSION_NAME: String = "kotlinD8Spec"
    }
}