/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.AbstractNodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.RESTORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.STORE_YARN_LOCK_NAME
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.Companion.UPGRADE_YARN_LOCK
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.detachedResolvable
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention
import java.io.File

abstract class AbstractYarnPlugin : Plugin<Project>, HasPlatformDisambiguate {

    abstract fun nodeJsRootApply(project: Project)

    abstract fun nodeJsRootExtension(project: Project): NodeJsRootExtension

    abstract fun nodeJsEnvSpec(project: Project): NodeJsEnvSpec

    abstract fun lockFileDirectory(projectDirectory: File): File

    override fun apply(project: Project): Unit = project.run {
        MultiplePluginDeclarationDetector.detect(project)

        check(project == project.rootProject) {
            "${this::class.java.name} can be applied only to root project"
        }

        nodeJsRootApply(project)

        val nodeJsRoot = nodeJsRootExtension(this)
        val nodeJs = nodeJsEnvSpec(this)

        val yarnSpec = project.extensions.createYarnEnvSpec(extensionName(YarnRootEnvSpec.YARN))

        val yarnRootExtension = this.extensions.create(
            extensionName(YarnRootExtension.YARN),
            YarnRootExtension::class.java,
            this,
            nodeJsRoot,
            yarnSpec,
        )

        yarnSpec.initializeYarnEnvSpec(objects, yarnRootExtension)

        yarnRootExtension.platform.value(nodeJs.platform)
            .disallowChanges()

        nodeJsRoot.packageManagerExtension.set(
            yarnRootExtension
        )

        val setupTask = registerTask<YarnSetupTask>(extensionName(YarnSetupTask.NAME), listOf(yarnSpec)) {
            with(nodeJs) {
                it.dependsOn(project.nodeJsSetupTaskProvider)
            }

            it.group = AbstractNodeJsRootPlugin.TASKS_GROUP_NAME
            it.description = "Download and install a local yarn version"

            it.configuration = it.ivyDependencyProvider.map { ivyDependency ->
                this.project.configurations.detachedResolvable(this.project.dependencies.create(ivyDependency))
                    .also { conf -> conf.isTransitive = false }
            }
        }

        val kotlinNpmInstall = tasks.named(extensionName(KotlinNpmInstallTask.NAME))
        kotlinNpmInstall.configure {
            it.dependsOn(setupTask)
            it.inputs.property("yarnIgnoreScripts", { yarnRootExtension.ignoreScripts })
        }

        yarnRootExtension.nodeJsEnvironment.value(
            nodeJs.env
        ).disallowChanges()

        tasks.register(extensionName("yarn" + CleanDataTask.NAME_SUFFIX), CleanDataTask::class.java) {
            it.cleanableStoreProvider = provider { yarnRootExtension.requireConfigured().cleanableStore }
            it.description = "Clean unused local yarn version"
        }

        yarnRootExtension.lockFileDirectory = lockFileDirectory(project.rootDir)

        tasks.register(extensionName(STORE_YARN_LOCK_NAME), YarnLockStoreTask::class.java) { task ->
            task.dependsOn(kotlinNpmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) })
            task.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            task.fileName.set(yarnRootExtension.lockFileName)

            task.lockFileMismatchReport.value(
                provider { yarnRootExtension.requireConfigured().yarnLockMismatchReport.toLockFileMismatchReport() }
            ).disallowChanges()
            task.reportNewLockFile.value(
                provider { yarnRootExtension.requireConfigured().reportNewYarnLock }
            ).disallowChanges()
            task.lockFileAutoReplace.value(
                provider { yarnRootExtension.requireConfigured().yarnLockAutoReplace }
            ).disallowChanges()
        }

        tasks.register(extensionName(UPGRADE_YARN_LOCK), YarnLockCopyTask::class.java) { task ->
            task.dependsOn(kotlinNpmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) })
            task.outputDirectory.set(yarnRootExtension.lockFileDirectory)
            task.fileName.set(yarnRootExtension.lockFileName)
        }

        tasks.register(extensionName(RESTORE_YARN_LOCK_NAME), YarnLockCopyTask::class.java) {
            val lockFile = yarnRootExtension.lockFileDirectory.resolve(yarnRootExtension.lockFileName)
            it.inputFile.set(yarnRootExtension.lockFileDirectory.resolve(yarnRootExtension.lockFileName))
            it.outputDirectory.set(nodeJsRoot.rootPackageDirectory)
            it.fileName.set(LockCopyTask.YARN_LOCK)
            it.onlyIf {
                lockFile.exists()
            }
        }

        yarnRootExtension.preInstallTasks.value(
            listOf(yarnRootExtension.restoreYarnLockTaskProvider)
        ).disallowChanges()

        yarnRootExtension.postInstallTasks.value(
            listOf(yarnRootExtension.storeYarnLockTaskProvider)
        ).disallowChanges()
    }

    private fun ExtensionContainer.createYarnEnvSpec(name: String): YarnRootEnvSpec {
        return create(
            name,
            YarnRootEnvSpec::class.java
        )
    }

    private fun YarnRootEnvSpec.initializeYarnEnvSpec(
        objectFactory: ObjectFactory,
        yarnRootExtension: YarnRootExtension,
    ) {
        download.convention(yarnRootExtension.downloadProperty)
        downloadBaseUrl.convention(yarnRootExtension.downloadBaseUrlProperty)
        installationDirectory.convention(yarnRootExtension.installationDirectory)
        version.convention(yarnRootExtension.versionProperty)
        command.convention(yarnRootExtension.commandProperty)
        platform.convention(yarnRootExtension.platform)
        ignoreScripts.convention(objectFactory.providerWithLazyConvention { yarnRootExtension.ignoreScripts })
        yarnLockMismatchReport.convention(objectFactory.providerWithLazyConvention { yarnRootExtension.yarnLockMismatchReport })
        reportNewYarnLock.convention(objectFactory.providerWithLazyConvention { yarnRootExtension.reportNewYarnLock })
        yarnLockAutoReplace.convention(objectFactory.providerWithLazyConvention { yarnRootExtension.yarnLockAutoReplace })
        resolutions.convention(
            objectFactory.listProperty<YarnResolution>().value(
                objectFactory.providerWithLazyConvention {
                    yarnRootExtension.resolutions
                }
            )
        )
    }
}
