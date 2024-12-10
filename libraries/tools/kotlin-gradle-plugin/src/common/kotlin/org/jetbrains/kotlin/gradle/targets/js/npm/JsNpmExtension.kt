/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsNodeJsRootPlugin

@Suppress("DEPRECATION")
open class JsNpmExtension(
    project: Project,
    nodeJsRoot: JsNodeJsRootExtension,
) : NpmExtension(
    project,
    nodeJsRoot
) {
    companion object {
        const val EXTENSION_NAME: String = "kotlinNpm"

        operator fun get(project: Project): JsNpmExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(JsNodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(EXTENSION_NAME) as JsNpmExtension
        }
    }
}