/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.unameExecResult
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.targets.js.MultiplePluginDeclarationDetector
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsForWasmPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.PACKAGE_JSON_UMBRELLA_TASK_NAME
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.implementing
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmCachesSetup
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnForWasmPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.tasks.CleanDataTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.castIsolatedKotlinPluginClassLoaderAware
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.onlyIfCompat
import org.jetbrains.kotlin.gradle.utils.providerWithLazyConvention

open class NodeJsRootForWasmPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        MultiplePluginDeclarationDetector.detect(project)

        check(project == project.rootProject) {
            "NodeJsRootPlugin can be applied only to root project"
        }

        project.plugins.apply(BasePlugin::class.java)

        val nodeJsRoot = project.extensions.create(
            extensionName(NodeJsRootExtension.EXTENSION_NAME),
            NodeJsRootExtension::class.java,
            project,
            { NodeJsForWasmPlugin.apply(project) },
            "wasm",
            "Wasm"
        )

        val npm = project.extensions.create(
            extensionName(NpmExtension.EXTENSION_NAME),
            NpmExtension::class.java,
            project,
            nodeJsRoot,
            "Wasm"
        )

        val nodeJs = NodeJsForWasmPlugin.apply(project)

        npm.nodeJsEnvironment.value(
            nodeJs.produceEnv(project.providers)
        ).disallowChanges()

        nodeJsRoot.packageManagerExtension.convention(
            npm
        )

        npm.lockFileDirectory.convention(
            project.layout.projectDirectory.dir("wasm").dir(LockCopyTask.KOTLIN_JS_STORE)
        )

        val gradleNodeModulesProvider: Provider<GradleNodeModulesCache> = GradleNodeModulesCache.registerIfAbsent(
            project,
            project.projectDir,
            nodeJsRoot.nodeModulesGradleCacheDirectory,
            "Wasm"
        )

        val setupFileHasherTask = project.registerTask<KotlinNpmCachesSetup>(extensionName(KotlinNpmCachesSetup.NAME)) {
            it.description = "Setup file hasher for caches"

            it.gradleNodeModules.set(gradleNodeModulesProvider)
        }

        val npmInstall = project.registerTask<KotlinNpmInstallTask>(extensionName(KotlinNpmInstallTask.NAME)) { npmInstall ->
            with(nodeJs) {
                npmInstall.dependsOn(project.nodeJsSetupTaskProvider)
            }
            npmInstall.dependsOn(setupFileHasherTask)
            npmInstall.group = TASKS_GROUP_NAME
            npmInstall.description = "Find, download and link NPM dependencies and projects"

            npmInstall.packageJsonFiles.value(
                project.provider {
                    nodeJsRoot.resolver
                        .projectResolvers.values
                        .flatMap { it.compilationResolvers }
                        .map { it.compilationNpmResolution }
                        .map { resolution ->
                            val name = resolution.npmProjectName
                            nodeJsRoot.projectPackagesDirectory.map { it.dir(name).file(NpmProject.PACKAGE_JSON) }.get()
                        }
                }
            ).disallowChanges()

            npmInstall.nodeJsEnvironment.value(
                nodeJs.produceEnv(project.providers)
                    .map {
                        asNodeJsEnvironment(nodeJsRoot, it)
                    }
            ).disallowChanges()

            npmInstall.packageManagerEnv.value(
                nodeJsRoot.packageManagerExtension.map { it.environment }
            ).disallowChanges()

            npmInstall.nodeModules.value(
                nodeJsRoot.rootPackageDirectory.map { it.dir("node_modules") }
            ).disallowChanges()

            npmInstall.additionalFiles.from(
                nodeJsRoot.packageManagerExtension.map { it.additionalInstallOutput }
            ).disallowChanges()

            npmInstall.preparedFiles.from(
                nodeJsRoot.packageManagerExtension.zip(npmInstall.nodeJsEnvironment) { npmApiExt, nodeJsEnvironment ->
                    npmApiExt.packageManager.preparedFiles(nodeJsEnvironment)
                }
            ).disallowChanges()

            npmInstall.onlyIfCompat("No package.json files for install") { task ->
                task as KotlinNpmInstallTask
                task.preparedFiles.all { file ->
                    file.exists()
                }
            }

            npmInstall.outputs.upToDateWhen {
                npmInstall.nodeModules.getFile().exists()
            }
        }

        project.registerTask<Task>(extensionName(PACKAGE_JSON_UMBRELLA_TASK_NAME))

        nodeJsRoot.resolver = KotlinRootNpmResolver(
            project.name,
            project.version.toString(),
            TasksRequirements(),
            nodeJsRoot.versions,
            nodeJsRoot.projectPackagesDirectory,
            nodeJsRoot.rootProjectDir,
            KotlinPlatformType.wasm
        )

        val objectFactory = project.objects

        val npmResolutionManager: Provider<KotlinNpmResolutionManager> = KotlinNpmResolutionManager.registerIfAbsent(
            project,
            objectFactory.providerWithLazyConvention {
                nodeJsRoot.resolver.close()
            },
            gradleNodeModulesProvider,
            nodeJsRoot.projectPackagesDirectory,
            "Wasm"
        )

        val rootPackageJson = project.tasks.register(extensionName(RootPackageJsonTask.NAME), RootPackageJsonTask::class.java) { task ->
            task.dependsOn(setupFileHasherTask)
            task.group = TASKS_GROUP_NAME
            task.description = "Create root package.json"

            task.npmResolutionManager.value(npmResolutionManager)
                .disallowChanges()

            task.packageJsonFiles.value(
                project.provider {
                    nodeJsRoot.resolver
                        .projectResolvers.values
                        .flatMap { it.compilationResolvers }
                        .map { it.compilationNpmResolution }
                        .map { resolution ->
                            val name = resolution.npmProjectName
                            nodeJsRoot.projectPackagesDirectory.map { it.dir(name).file(NpmProject.PACKAGE_JSON) }.get()
                        }
                }
            ).disallowChanges()

            task.nodeJsEnvironment.value(
                nodeJs.produceEnv(project.providers)
                    .map {
                        asNodeJsEnvironment(nodeJsRoot, it)
                    }
            ).disallowChanges()

            task.rootPackageJsonFile.value(
                nodeJsRoot.rootPackageDirectory.map { it.file(NpmProject.PACKAGE_JSON) }
            ).disallowChanges()

            task.packageManagerEnv.value(
                nodeJsRoot.packageManagerExtension.map { it.environment }
            ).disallowChanges()

            task.onlyIfCompat("Prepare NPM project only in configuring state") {
                it as RootPackageJsonTask
                it.npmResolutionManager.get().isConfiguringState()
            }
        }

        configureRequiresNpmDependencies(project, rootPackageJson)

        val packageJsonUmbrella = nodeJsRoot
            .packageJsonUmbrellaTaskProvider

        nodeJsRoot.rootPackageJsonTaskProvider.configure {
            it.dependsOn(packageJsonUmbrella)
        }

        npmInstall.configure {
            it.dependsOn(rootPackageJson)
            it.inputs.property("npmIgnoreScripts", { npm.ignoreScripts })
        }

        project.tasks.register(extensionName(LockCopyTask.STORE_PACKAGE_LOCK_NAME), LockStoreTask::class.java) { task ->
            task.dependsOn(npmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.PACKAGE_LOCK) })

            task.additionalInputFiles.from(
                nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )
            task.additionalInputFiles.from(
                task.outputDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )

            task.outputDirectory.set(npm.lockFileDirectory)
            task.fileName.set(npm.lockFileName)

            task.lockFileMismatchReport.value(
                project.provider { npm.requireConfigured().packageLockMismatchReport }
            ).disallowChanges()
            task.reportNewLockFile.value(
                project.provider { npm.requireConfigured().reportNewPackageLock }
            ).disallowChanges()
            task.lockFileAutoReplace.value(
                project.provider { npm.requireConfigured().packageLockAutoReplace }
            ).disallowChanges()
        }

        project.tasks.register(extensionName(LockCopyTask.UPGRADE_PACKAGE_LOCK), LockStoreTask::class.java) { task ->
            task.dependsOn(npmInstall)
            task.inputFile.set(nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.PACKAGE_LOCK) })
            task.outputDirectory.set(npm.lockFileDirectory)
            task.fileName.set(npm.lockFileName)

            task.additionalInputFiles.from(
                nodeJsRoot.rootPackageDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )
            task.additionalInputFiles.from(
                task.outputDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )

            task.lockFileMismatchReport.value(
                LockFileMismatchReport.NONE
            ).disallowChanges()
            task.reportNewLockFile.value(
                false
            ).disallowChanges()
            task.lockFileAutoReplace.value(
                true
            ).disallowChanges()
        }

        project.tasks.register(extensionName(LockCopyTask.RESTORE_PACKAGE_LOCK_NAME), LockCopyTask::class.java) { task ->
            task.inputFile.set(
                npm.lockFileDirectory.flatMap { dir ->
                    dir.file(npm.lockFileName)
                }
            )
            task.additionalInputFiles.from(
                npm.lockFileDirectory.map { it.file(LockCopyTask.YARN_LOCK) }
            )
            task.outputDirectory.set(nodeJsRoot.rootPackageDirectory)
            task.fileName.set(LockCopyTask.PACKAGE_LOCK)
            task.onlyIf {
                val inputFileExists = task.inputFile.getOrNull()?.asFile?.exists() == true
                // Workaround for "skip if not exists"
                // https://github.com/gradle/gradle/issues/2919
                if (!inputFileExists) {
                    task.inputFile.set(null as RegularFile?)
                }
                inputFileExists || task.additionalInputFiles.files.any { it.exists() }
            }
        }

        npm.preInstallTasks.value(
            listOf(npm.restorePackageLockTaskProvider)
        ).disallowChanges()

        npm.postInstallTasks.value(
            listOf(npm.storePackageLockTaskProvider)
        ).disallowChanges()

        npmInstall.configure {
            it.dependsOn(nodeJsRoot.packageManagerExtension.map { it.preInstallTasks })
        }

        npmInstall.configure {
            it.npmResolutionManager.value(npmResolutionManager).disallowChanges()
        }

        project.tasks.register("node" + CleanDataTask.NAME_SUFFIX + "Wasm", CleanDataTask::class.java) {
            it.cleanableStoreProvider = nodeJs.produceEnv(project.providers).map { it.cleanableStore }
            it.group = TASKS_GROUP_NAME
            it.description = "Clean unused local node version"
        }

        val propertiesProvider = PropertiesProvider(project)

        if (propertiesProvider.yarn) {
            project.plugins.apply(YarnForWasmPlugin::class.java)
        }
    }

    private fun extensionName(baseName: String): String =
        baseName + "Wasm"

    // Yes, we need to break Task Configuration Avoidance here
    // In case when we need to create package.json's files and execute kotlinNpmInstall,
    // We need to configure all RequiresNpmDependencies tasks to install them,
    // Because we need to persist lock file
    // We execute this block in configure phase of rootPackageJson to be sure,
    // That Task Configuration Avoidance will not be broken for tasks not related with NPM installing
    // https://youtrack.jetbrains.com/issue/KT-48241
    private fun configureRequiresNpmDependencies(
        project: Project,
        rootPackageJson: TaskProvider<RootPackageJsonTask>,
    ) {
        val fn: (Project) -> Unit = {
            it.tasks.implementing(RequiresNpmDependencies::class)
                .forEach {}
        }
        rootPackageJson.configure {
            project.allprojects
                .forEach { project ->
                    if (it.state.executed) {
                        fn(project)
                    }
                }
        }

        project.allprojects
            .forEach {
                if (!it.state.executed) {
                    it.afterEvaluate { project ->
                        rootPackageJson.configure {
                            fn(project)
                        }
                    }
                }
            }
    }

    companion object {
        const val TASKS_GROUP_NAME: String = "nodeJs"

        fun apply(rootProject: Project): NodeJsRootExtension {
            check(rootProject == rootProject.rootProject)
            rootProject.plugins.apply(NodeJsRootForWasmPlugin::class.java)
            return rootProject.extensions.getByName(NodeJsRootExtension.EXTENSION_NAME + "Wasm") as NodeJsRootExtension
        }

        val Project.kotlinNodeJsRootExtension: NodeJsRootExtension
            get() = extensions.getByName(NodeJsRootExtension.EXTENSION_NAME + "Wasm").castIsolatedKotlinPluginClassLoaderAware()

        val Project.kotlinNpmResolutionManager: Provider<KotlinNpmResolutionManager>
            get() {
                return project.gradle.sharedServices.registerIfAbsent(
                    KotlinNpmResolutionManager::class.java.name + "Wasm",
                    KotlinNpmResolutionManager::class.java
                ) {
                    error("Must be already registered")
                }
            }
    }
}
