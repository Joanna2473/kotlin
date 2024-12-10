/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.npm.KotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.JsNpmExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.JsYarnPlugin
import org.jetbrains.kotlin.gradle.targets.web.nodejs.NodeJsRootPluginApplier
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware

@Suppress("DEPRECATION")
open class JsNodeJsRootPlugin : NodeJsRootPlugin() {

    override fun apply(target: Project) {
        NodeJsRootPluginApplier(
            platformDisambiguate = JsPlatformDisambiguate,
            nodeJsRootKlass = JsNodeJsRootExtension::class,
            nodeJsRootName = JsNodeJsRootExtension.EXTENSION_NAME,
            npmKlass = JsNpmExtension::class,
            npmName = JsNpmExtension.EXTENSION_NAME,
            rootDirectoryName = JsPlatformDisambiguate.jsPlatform,
            lockFileDirectory = { it.dir(LockCopyTask.KOTLIN_JS_STORE) },
            singleNodeJsPluginApply = { JsNodeJsPlugin.apply(it) },
            yarnPlugin = JsYarnPlugin::class,
            platformType = KotlinPlatformType.js,
        ).apply(target)
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "nodeJs"

        fun apply(rootProject: Project): JsNodeJsRootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(JsNodeJsRootPlugin::class.java)
            return rootProject.extensions.getByName(JsNodeJsRootExtension.EXTENSION_NAME) as JsNodeJsRootExtension
        }

        val Project.kotlinNodeJsRootExtension: JsNodeJsRootExtension
            get() = extensions.getByName(JsNodeJsRootExtension.EXTENSION_NAME).castIsolatedKotlinPluginClassLoaderAware()

        val Project.kotlinNpmResolutionManager: Provider<KotlinNpmResolutionManager>
            get() {
                return project.gradle.sharedServices.registerIfAbsent(
                    KotlinNpmResolutionManager::class.java.name,
                    KotlinNpmResolutionManager::class.java
                ) {
                    error("Must be already registered")
                }
            }
    }
}
