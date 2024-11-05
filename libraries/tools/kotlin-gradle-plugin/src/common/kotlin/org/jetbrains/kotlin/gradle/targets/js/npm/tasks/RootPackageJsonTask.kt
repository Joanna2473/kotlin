/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsForWasmPlugin.Companion.kotlinNodeJsEnvSpec as kotlinNodeJsForWasmEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.Companion.kotlinNodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootForWasmPlugin.Companion.kotlinNodeJsRootExtension as kotlinNodeJsForWasmRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager
import org.jetbrains.kotlin.gradle.targets.js.npm.asNodeJsEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinRootNpmResolver
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File

@DisableCachingByDefault
abstract class RootPackageJsonTask :
    DefaultTask(),
    UsesKotlinNpmResolutionManager {
    init {
        check(project == project.rootProject)
    }

    // Only in configuration phase
    // Not part of configuration caching

    @get:Internal
    abstract val getWasm: Property<Boolean>

//    private val nodeJsRoot
//        get() = project.rootProject.kotlinNodeJsRootExtension

//    private val nodeJs
//        get() = project.rootProject.kotlinNodeJsEnvSpec

    private val rootResolver: KotlinRootNpmResolver
        get() = if (getWasm.get()) {
            println("$name rootResolver Wasm")

            project.rootProject.kotlinNodeJsForWasmRootExtension.resolver
        } else {
            println("$name rootResolver js")

            project.rootProject.kotlinNodeJsRootExtension.resolver
        }

    private val packagesDir: Provider<Directory>
        get() = if (getWasm.get()) {
            println("$name packagesDir Wasm")
            project.rootProject.kotlinNodeJsForWasmRootExtension.projectPackagesDirectory
        } else {
            println("$name packagesDir js")
            project.rootProject.kotlinNodeJsRootExtension.projectPackagesDirectory
        }

    // -----

    private val nodeJsEnvironment by lazy {
        if (getWasm.get()) {
            println("$name nodeJsEnvironment Wasm")
            asNodeJsEnvironment(
                project.rootProject.kotlinNodeJsForWasmRootExtension,
                project.rootProject.kotlinNodeJsForWasmEnvSpec.produceEnv(project.providers).get()
            )
        } else {
            println("$name nodeJsEnvironment js")
            asNodeJsEnvironment(
                project.rootProject.kotlinNodeJsRootExtension,
                project.rootProject.kotlinNodeJsEnvSpec.produceEnv(project.providers).get()
            )
        }
    }

    private val packageManagerEnv by lazy {
        if (getWasm.get()) {
            println("$name packageManagerEnv Wasm")
            project.rootProject.kotlinNodeJsForWasmRootExtension.packageManagerExtension.get().environment
        } else {
            println("$name packageManagerEnv js")
            project.rootProject.kotlinNodeJsRootExtension.packageManagerExtension.get().environment
        }
    }

    private val wasmPackageDir by lazy { project.rootProject.kotlinNodeJsForWasmRootExtension.rootPackageDirectory }
    private val jsPackageDir by lazy { project.rootProject.kotlinNodeJsRootExtension.rootPackageDirectory }

    @get:OutputFile
    val rootPackageJsonFile: Provider<RegularFile> = getWasm.flatMap {
        if (it) {
            println("$name rootPackageJsonFile Wasm")
            wasmPackageDir.map { it.file(NpmProject.PACKAGE_JSON) }
        } else {
            println("$name rootPackageJsonFile js")

            jsPackageDir.map { it.file(NpmProject.PACKAGE_JSON) }
        }
    }

    @Deprecated(
        "This property is deprecated and will be removed in future. Use rootPackageJsonFile instead",
        replaceWith = ReplaceWith("rootPackageJsonFile")
    )
    @get:Internal
    val rootPackageJson: File
        get() = rootPackageJsonFile.getFile()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    val packageJsonFiles: List<RegularFile> by lazy {
        rootResolver.projectResolvers.values
            .flatMap { it.compilationResolvers }
            .map { it.compilationNpmResolution }
            .map { resolution ->
                val name = resolution.npmProjectName
                packagesDir.map { it.dir(name).file(NpmProject.PACKAGE_JSON) }.get()
            }
    }

    @TaskAction
    fun resolve() {
        npmResolutionManager.get().prepare(logger, nodeJsEnvironment, packageManagerEnv)
    }

    companion object {
        const val NAME = "rootPackageJson"
    }
}