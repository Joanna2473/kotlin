/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsNodeJsRootExtension

@Suppress("DEPRECATION")
open class JsYarnRootExtension(
    project: Project,
    nodeJsRoot: JsNodeJsRootExtension,
    yarnSpec: JsYarnRootEnvSpec,
) : YarnRootExtension(
    project,
    nodeJsRoot,
    yarnSpec,
) {
    companion object {
        const val YARN: String = "kotlinYarn"

        operator fun get(project: Project): JsYarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(JsYarnPlugin::class.java)
            return rootProject.extensions.getByName(YARN) as JsYarnRootExtension
        }
    }
}

val Project.yarn: JsYarnRootExtension
    get() = JsYarnRootExtension[this]