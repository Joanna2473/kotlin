/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project

@Deprecated(
    "Use JsYarnPlugin instead",
    ReplaceWith(
        expression = "JsYarnPlugin",
        "org.jetbrains.kotlin.gradle.targets.js.yarn.JsYarnPlugin"
    )
)
open class YarnPlugin : JsYarnPlugin() {
    companion object {
        fun apply(project: Project): JsYarnRootExtension =
            JsYarnPlugin.apply(project)

        const val STORE_YARN_LOCK_NAME = JsYarnPlugin.STORE_YARN_LOCK_NAME
        const val RESTORE_YARN_LOCK_NAME = JsYarnPlugin.RESTORE_YARN_LOCK_NAME
        const val UPGRADE_YARN_LOCK = JsYarnPlugin.UPGRADE_YARN_LOCK
        const val YARN_LOCK_MISMATCH_MESSAGE = JsYarnPlugin.YARN_LOCK_MISMATCH_MESSAGE
    }
}
