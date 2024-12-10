/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.web.npm.CommonNpmResolverPlugin

@Deprecated(
    "Use NpmResolverPlugin instead",
    ReplaceWith(
        "NpmResolverPlugin",
        "org.jetbrains.kotlin.gradle.targets.js.npm.NpmResolverPlugin"
    )
)
open class NpmResolverPlugin : CommonNpmResolverPlugin {
    override fun apply(project: Project) {
        project.plugins.apply(JsNpmResolverPlugin::class.java)
    }

    companion object {
        fun apply(project: Project) = JsNpmResolverPlugin.apply(project)
    }
}