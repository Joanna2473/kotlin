/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsForWasmPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin.Companion.wasmPlatform
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask

open class YarnForWasmPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        YarnPluginApplier(
            platformDisambiguate = wasmPlatform,
            nodeJsRootApply = { NodeJsRootForWasmPlugin.apply(it) },
            nodeJsRootExtension = { it.kotlinNodeJsRootExtension },
            nodeJsEnvSpec = { it.kotlinNodeJsEnvSpec },
            lockFileDirectory = { it.resolve(LockCopyTask.KOTLIN_JS_STORE).resolve(platformDisambiguate) },
        )
    }

    companion object : HasPlatformDisambiguate {
        fun apply(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnForWasmPlugin::class.java)
            return rootProject.extensions.getByName(extensionName(YarnRootExtension.YARN)) as YarnRootExtension
        }

        override val platformDisambiguate: String
            get() = wasmPlatform
    }
}
