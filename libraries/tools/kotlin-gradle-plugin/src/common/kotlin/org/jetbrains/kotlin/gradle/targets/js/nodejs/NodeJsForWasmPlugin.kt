/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin.Companion.wasmPlatform
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

open class NodeJsForWasmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        NodeJsPluginApplier(
            platformDisambiguate = wasmPlatform,
            nodeJsRootApply = { NodeJsRootForWasmPlugin.apply(it) }
        )
    }

    companion object : HasPlatformDisambiguate {
        fun apply(project: Project): NodeJsEnvSpec {
            project.plugins.apply(NodeJsForWasmPlugin::class.java)
            return project.extensions.getByName(extensionName(NodeJsEnvSpec.EXTENSION_NAME)) as NodeJsEnvSpec
        }

        val Project.kotlinNodeJsEnvSpec: NodeJsEnvSpec
            get() = extensions.getByName(extensionName(NodeJsEnvSpec.EXTENSION_NAME)).castIsolatedKotlinPluginClassLoaderAware()

        override val platformDisambiguate: String
            get() = wasmPlatform
    }
}
