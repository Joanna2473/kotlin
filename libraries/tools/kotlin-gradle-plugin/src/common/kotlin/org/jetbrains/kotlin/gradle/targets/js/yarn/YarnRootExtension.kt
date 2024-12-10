/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.web.yarn.AbstractYarnRootExtension

@Deprecated(
    "Use JsYarnRootExtension instead",
    ReplaceWith(
        "JsYarnRootExtension",
        "org.jetbrains.kotlin.gradle.targets.js.yarn.JsYarnRootExtension"
    )
)
open class YarnRootExtension(
    project: Project,
    nodeJsRoot: JsNodeJsRootExtension,
    yarnSpec: JsYarnRootEnvSpec,
) : AbstractYarnRootExtension(
    project,
    nodeJsRoot,
    yarnSpec,
) {
    companion object {
        const val YARN: String = JsYarnRootExtension.YARN

        operator fun get(project: Project): JsYarnRootExtension =
            JsYarnRootExtension[project]
    }
}