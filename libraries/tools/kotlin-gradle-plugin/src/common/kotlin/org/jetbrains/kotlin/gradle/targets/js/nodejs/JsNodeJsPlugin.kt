/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NodeJsPluginApplier
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

@Suppress("DEPRECATION")
open class JsNodeJsPlugin : NodeJsPlugin() {
    override fun apply(target: Project) {
        NodeJsPluginApplier(
            platformDisambiguate = JsPlatformDisambiguate,
            nodeJsEnvSpecKlass = JsNodeJsEnvSpec::class,
            nodeJsEnvSpecName = JsNodeJsEnvSpec.EXTENSION_NAME,
            nodeJsRootApply = { JsNodeJsRootPlugin.apply(it) }
        ).apply(target)
    }

    companion object {
        fun apply(project: Project): JsNodeJsEnvSpec {
            project.plugins.apply(JsNodeJsPlugin::class.java)
            return project.extensions.getByName(JsNodeJsEnvSpec.EXTENSION_NAME) as JsNodeJsEnvSpec
        }

        val Project.kotlinNodeJsEnvSpec: JsNodeJsEnvSpec
            get() = extensions.getByName(JsNodeJsEnvSpec.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()
    }
}
