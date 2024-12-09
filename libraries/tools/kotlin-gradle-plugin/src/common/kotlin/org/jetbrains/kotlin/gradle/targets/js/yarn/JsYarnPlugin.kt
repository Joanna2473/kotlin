/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.yarn.CommonYarnPlugin

open class JsYarnPlugin : CommonYarnPlugin {

    override fun apply(target: Project) {
        YarnPluginApplier(
            platformDisambiguate = JsPlatformDisambiguate,
            yarnRootKlass = JsYarnRootExtension::class,
            yarnRootName = JsYarnRootExtension.YARN,
            yarnEnvSpecKlass = JsYarnRootEnvSpec::class,
            yarnEnvSpecName = JsYarnRootEnvSpec.YARN,
            nodeJsRootApply = { NodeJsRootPlugin.apply(it) },
            nodeJsRootExtension = { it.kotlinNodeJsRootExtension },
            nodeJsEnvSpec = { it.kotlinNodeJsEnvSpec },
            lockFileDirectory = { it.resolve(LockCopyTask.KOTLIN_JS_STORE) },
        ).apply(target)
    }

    companion object {
        fun apply(project: Project): JsYarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(JsYarnPlugin::class.java)
            return rootProject.extensions.getByName(JsYarnRootExtension.YARN) as JsYarnRootExtension
        }

        const val STORE_YARN_LOCK_NAME = "kotlinStoreYarnLock"
        const val RESTORE_YARN_LOCK_NAME = "kotlinRestoreYarnLock"
        const val UPGRADE_YARN_LOCK = "kotlinUpgradeYarnLock"
        const val YARN_LOCK_MISMATCH_MESSAGE = "Lock file was changed. Run the `${UPGRADE_YARN_LOCK}` task to actualize lock file"
    }
}
