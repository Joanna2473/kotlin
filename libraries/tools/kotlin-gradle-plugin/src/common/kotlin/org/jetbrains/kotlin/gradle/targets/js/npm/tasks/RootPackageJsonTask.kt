/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.NodeJsEnvironment
import org.jetbrains.kotlin.gradle.targets.js.npm.UsesKotlinNpmResolutionManager

@DisableCachingByDefault
abstract class RootPackageJsonTask :
    DefaultTask(),
    UsesKotlinNpmResolutionManager {
    init {
        check(project == project.rootProject)
    }

    @get:Internal
    internal abstract val nodeJsEnvironment: Property<NodeJsEnvironment>

    @get:Internal
    internal abstract val packageManagerEnv: Property<PackageManagerEnvironment>

    @get:OutputFile
    val rootPackageJsonFile: Property<RegularFile> = project.objects.fileProperty()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    abstract val packageJsonFiles: ListProperty<RegularFile>

    @TaskAction
    fun resolve() {
        npmResolutionManager.get().prepare(logger, nodeJsEnvironment.get(), packageManagerEnv.get())
    }

    companion object {
        const val NAME = "rootPackageJson"
    }
}