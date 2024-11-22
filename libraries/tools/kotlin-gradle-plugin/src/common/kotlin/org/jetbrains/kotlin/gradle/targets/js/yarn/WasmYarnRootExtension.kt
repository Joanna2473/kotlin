/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmPlatformDisambiguate

open class WasmYarnRootExtension(
    project: Project,
    nodeJsRoot: WasmNodeJsRootExtension,
    yarnSpec: WasmYarnRootEnvSpec,
) : AbstractYarnRootExtension(
    project,
    nodeJsRoot,
    yarnSpec,
) {
    companion object : HasPlatformDisambiguate by WasmPlatformDisambiguate {
        val YARN: String
            get() = extensionName("kotlinYarn")

        operator fun get(project: Project): WasmYarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YARN) as WasmYarnRootExtension
        }
    }
}