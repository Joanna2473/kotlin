/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.jetbrains.kotlin.buildtools.internal.KotlinBuildToolsInternalJdkUtils
import org.jetbrains.kotlin.buildtools.internal.getJdkClassesClassLoader

internal class SharedClassLoader(
    private val sharedClassLoader: ClassLoader,
    private val sharedPackage: String,
) : ClassLoader(@OptIn(KotlinBuildToolsInternalJdkUtils::class) (getJdkClassesClassLoader())) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return if (name.startsWith(sharedPackage)) {
            sharedClassLoader.loadClass(name)
        } else {
            super.loadClass(name, resolve)
        }
    }
}