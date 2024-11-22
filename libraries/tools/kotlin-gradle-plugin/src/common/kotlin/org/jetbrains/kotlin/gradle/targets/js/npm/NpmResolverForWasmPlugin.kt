/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.WasmNodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.implementing
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.utils.whenEvaluated

class NpmResolverForWasmPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val nodeJsRoot = WasmNodeJsRootPlugin.apply(project.rootProject)
        WasmNodeJsPlugin.apply(project)
        project.rootProject.kotlinNodeJsRootExtension.resolver.addProject(project)
        val kotlinNodeJsTaskProvidersExtension = project.rootProject.kotlinNodeJsRootExtension
        project.whenEvaluated {
            project.tasks.implementing(RequiresNpmDependencies::class)
                .configureEach { task ->
                    if (task.enabled) {
                        task as RequiresNpmDependencies
                        // KotlinJsTest delegates npm dependencies to testFramework,
                        // which can be defined after this configure action
                        if (task !is KotlinJsTest) {
                            nodeJsRoot.taskRequirements.addTaskRequirements(task)
                        }
                        task.dependsOn(
                            kotlinNodeJsTaskProvidersExtension.npmInstallTaskProvider,
                        )

                        task.dependsOn(nodeJsRoot.packageManagerExtension.map { it.postInstallTasks })
                    }
                }
        }
    }

    companion object {
        fun apply(project: Project) {
            project.plugins.apply(NpmResolverForWasmPlugin::class.java)
        }
    }
}