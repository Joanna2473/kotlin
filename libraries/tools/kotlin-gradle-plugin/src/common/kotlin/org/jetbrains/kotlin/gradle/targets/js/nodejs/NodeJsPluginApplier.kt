/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention

internal class NodeJsPluginApplier(
    override val platformDisambiguate: String?,
    private val nodeJsRootApply: (project: Project) -> NodeJsRootExtension,
) : HasPlatformDisambiguate {

    fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        val nodeJs = project.createNodeJsEnvSpec {
            nodeJsRootApply(project.rootProject)
        }

        project.registerTask<NodeJsSetupTask>(extensionName(NodeJsSetupTask.NAME), listOf(nodeJs)) {
            it.group = NodeJsRootPlugin.TASKS_GROUP_NAME
            it.description = "Download and install a local node/npm version"
            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                project.configurations.detachedConfiguration(project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Project.createNodeJsEnvSpec(
        nodeJsConstructor: () -> NodeJsRootExtension,
    ): NodeJsEnvSpec {
        val extensions = extensions
        val objects = objects

        return extensions.create(
            extensionName(NodeJsEnvSpec.EXTENSION_NAME),
            NodeJsEnvSpec::class.java,
        ).apply {
            platformDisambiguate.set(this@NodeJsPluginApplier.platformDisambiguate)

            installationDirectory.convention(
                objects.directoryProperty().fileProvider(
                    objects.providerWithLazyConvention {
                        nodeJsConstructor().installationDir
                    }
                )
            )
            download.convention(objects.providerWithLazyConvention { nodeJsConstructor().download })
            // set instead of convention because it is possible to have null value
            downloadBaseUrl.set(objects.providerWithLazyConvention { nodeJsConstructor().downloadBaseUrl })
            version.convention(objects.providerWithLazyConvention { nodeJsConstructor().version })
            command.convention(objects.providerWithLazyConvention { nodeJsConstructor().command })

            addPlatform(this@createNodeJsEnvSpec, this)
        }
    }

    private fun addPlatform(project: Project, extension: NodeJsEnvSpec) {
        val uname = project.providers
            .unameExecResult

        extension.platform.value(
            project.providers.systemProperty("os.name")
                .zip(
                    project.providers.systemProperty("os.arch")
                ) { name, arch ->
                    parsePlatform(name, arch, uname)
                }
        ).disallowChanges()
    }
}