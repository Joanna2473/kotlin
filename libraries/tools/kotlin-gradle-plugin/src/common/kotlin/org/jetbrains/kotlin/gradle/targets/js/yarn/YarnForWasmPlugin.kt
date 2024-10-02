/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsForWasmPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin.Companion.wasmPlatform
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import java.io.File

open class YarnForWasmPlugin : AbstractYarnPlugin() {
    override val platformDisambiguate: String
        get() = wasmPlatform

    override fun nodeJsRootApply(project: Project) {
        NodeJsRootForWasmPlugin.apply(project)
    }

    override fun nodeJsRootExtension(project: Project): NodeJsRootExtension =
        project.kotlinNodeJsRootExtension

    override fun nodeJsEnvSpec(project: Project): NodeJsEnvSpec =
        project.kotlinNodeJsEnvSpec

    override fun lockFileDirectory(projectDirectory: File): File =
        projectDirectory.resolve(LockCopyTask.KOTLIN_JS_STORE).resolve(platformDisambiguate)

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
