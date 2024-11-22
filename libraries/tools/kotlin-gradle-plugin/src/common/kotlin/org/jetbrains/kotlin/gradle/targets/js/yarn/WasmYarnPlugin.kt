/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmNodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask

open class WasmYarnPlugin : Plugin<Project> {
    override fun apply(target: Project) {

        YarnPluginApplier(
            platformDisambiguate = WasmPlatformDisambiguate,
            yarnRootKlass = WasmYarnRootExtension::class,
            yarnRootName = WasmYarnRootExtension.YARN,
            yarnEnvSpecKlass = WasmYarnRootEnvSpec::class,
            yarnEnvSpecName = WasmYarnRootEnvSpec.YARN,
            nodeJsRootApply = { WasmNodeJsRootPlugin.apply(it) },
            nodeJsRootExtension = { it.kotlinNodeJsRootExtension },
            nodeJsEnvSpec = { it.kotlinNodeJsEnvSpec },
            lockFileDirectory = { it.resolve(LockCopyTask.KOTLIN_JS_STORE).resolve(WasmPlatformDisambiguate.platformDisambiguate) },
        ).apply(target)
    }

    companion object : HasPlatformDisambiguate by WasmPlatformDisambiguate {
        fun apply(project: Project): WasmYarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(WasmYarnPlugin::class.java)
            return rootProject.extensions.getByName(extensionName(YarnRootExtension.YARN)) as WasmYarnRootExtension
        }
    }
}
