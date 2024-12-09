/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension

@Deprecated("Use JsYarnRootExtension instead", ReplaceWith("JsYarnRootExtension"))
open class YarnRootExtension(
    project: Project,
    nodeJsRoot: NodeJsRootExtension,
    yarnSpec: JsYarnRootEnvSpec,
) : AbstractYarnRootExtension(
    project,
    nodeJsRoot,
    yarnSpec,
) {
    companion object {
        const val YARN: String = JsYarnRootExtension.YARN

        operator fun get(project: Project): JsYarnRootExtension =
            JsYarnRootExtension.get(project)
    }
}