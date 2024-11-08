/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager

@DisableCachingByDefault
abstract class KotlinNpmInstallTask :
    DefaultTask(),
    UsesKotlinNpmResolutionManager {
    init {
        check(project == project.rootProject)
    }

    @get:Internal
    internal abstract val nodeJsEnvironment: Property<NodeJsEnvironment>

    @get:Internal
    internal abstract val packageManagerEnv: Property<PackageManagerEnvironment>

    @Input
    val args: MutableList<String> = mutableListOf()

    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    abstract val preparedFiles: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    abstract val packageJsonFiles: ListProperty<RegularFile>

    @get:OutputFiles
    abstract val additionalFiles: ConfigurableFileCollection

    // node_modules as OutputDirectory is performance problematic
    // so input will only be existence of its directory
    @get:Internal
    abstract val nodeModules: DirectoryProperty

    @TaskAction
    fun resolve() {
        npmResolutionManager.get()
            .installIfNeeded(
                args = args,
                services = services,
                logger = logger,
                nodeJsEnvironment.get(),
                packageManagerEnv.get(),
            ) ?: throw (npmResolutionManager.get().state as KotlinNpmResolutionManager.ResolutionState.Error).wrappedException
    }

    companion object {
        const val NAME = "kotlinNpmInstall"
    }
}