/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import java.io.File

open class YarnPlugin : AbstractYarnPlugin() {
    override val platformDisambiguate: String?
        get() = null

    override fun nodeJsRootApply(project: Project) {
        NodeJsRootPlugin.apply(project)
    }

    override fun nodeJsRootExtension(project: Project): NodeJsRootExtension =
        project.kotlinNodeJsRootExtension

    override fun nodeJsEnvSpec(project: Project): NodeJsEnvSpec =
        project.kotlinNodeJsEnvSpec

    override fun lockFileDirectory(projectDirectory: File): File =
        projectDirectory.resolve(LockCopyTask.KOTLIN_JS_STORE)

    companion object {
        fun apply(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YarnRootExtension.YARN) as YarnRootExtension
        }

        const val STORE_YARN_LOCK_NAME = "kotlinStoreYarnLock"
        const val RESTORE_YARN_LOCK_NAME = "kotlinRestoreYarnLock"
        const val UPGRADE_YARN_LOCK = "kotlinUpgradeYarnLock"
        const val YARN_LOCK_MISMATCH_MESSAGE = "Lock file was changed. Run the `${UPGRADE_YARN_LOCK}` task to actualize lock file"
    }
}
