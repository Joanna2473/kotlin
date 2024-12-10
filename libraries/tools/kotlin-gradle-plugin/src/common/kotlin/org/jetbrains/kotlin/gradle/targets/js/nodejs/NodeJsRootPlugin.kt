/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager

@Deprecated(
    "Use JsNodeJsRootPlugin instead",
    ReplaceWith(
        expression = "JsNodeJsRootPlugin",
        "org.jetbrains.kotlin.gradle.targets.js.nodejs.JsNodeJsRootPlugin"
    )
)
open class NodeJsRootPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.apply(JsNodeJsRootPlugin::class.java)
    }

    companion object {
        const val TASKS_GROUP_NAME: String = JsNodeJsRootPlugin.TASKS_GROUP_NAME

        fun apply(rootProject: Project): JsNodeJsRootExtension =
            JsNodeJsRootPlugin.apply(rootProject)

        val Project.kotlinNodeJsRootExtension: JsNodeJsRootExtension
            get() = with(JsNodeJsRootPlugin.Companion) {
                this@kotlinNodeJsRootExtension.kotlinNodeJsRootExtension
            }

        val Project.kotlinNpmResolutionManager: Provider<KotlinNpmResolutionManager>
            get() {
                return with(JsNodeJsRootPlugin.Companion) {
                    this@kotlinNpmResolutionManager.kotlinNpmResolutionManager
                }
            }
    }
}
