/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project

@Deprecated(
    "Use JsNodeJsPlugin instead",
    ReplaceWith(
        expression = "JsNodeJsPlugin",
        "org.jetbrains.kotlin.gradle.targets.js.nodejs.JsNodeJsPlugin"
    )
)
open class NodeJsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply(JsNodeJsPlugin::class.java)
    }

    companion object {
        fun apply(project: Project): JsNodeJsEnvSpec =
            JsNodeJsPlugin.apply(project)

        val Project.kotlinNodeJsEnvSpec: JsNodeJsEnvSpec
            get() = with(JsNodeJsPlugin.Companion) {
                this@kotlinNodeJsEnvSpec.kotlinNodeJsEnvSpec
            }
    }
}
