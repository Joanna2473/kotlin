/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsNodeJsRootPlugin

class NpmResolverPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        NpmResolverPluginApplier(
            { JsNodeJsRootPlugin.apply(project.rootProject) },
            { JsNodeJsPlugin.apply(project) },
        ).apply(project)
    }

    companion object {
        fun apply(project: Project) {
            project.plugins.apply(NpmResolverPlugin::class.java)
        }
    }
}