/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import kotlin.reflect.KClass

open class NodeJsRootPlugin : AbstractNodeJsRootPlugin() {

    override val rootDirectoryName: String
        get() = jsPlatform

    override val platformDisambiguate: String?
        get() = null

    override fun lockFileDirectory(projectDirectory: Directory): Directory {
        return projectDirectory.dir(LockCopyTask.KOTLIN_JS_STORE)
    }

    override fun singleNodeJsPluginApply(project: Project): NodeJsEnvSpec =
        NodeJsPlugin.apply(project)

    override val yarnPlugin: KClass<out Plugin<Project>> =
        YarnPlugin::class

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    companion object {
        fun apply(rootProject: Project): NodeJsRootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(NodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(NodeJsRootExtension.EXTENSION_NAME) as NodeJsRootExtension
        }

        val Project.kotlinNodeJsRootExtension: NodeJsRootExtension
            get() = extensions.getByName(NodeJsRootExtension.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

        val Project.kotlinNpmResolutionManager: Provider<KotlinNpmResolutionManager>
            get() {
                return project.gradle.sharedServices.registerIfAbsent(
                    KotlinNpmResolutionManager::class.java.name,
                    KotlinNpmResolutionManager::class.java
                ) {
                    error("Must be already registered")
                }
            }

        val jsPlatform: String
            get() = KotlinPlatformType.js.name
    }
}
