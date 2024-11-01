/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.gradle.plugin.abi

import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.utils.DeprecatedAndroidBaseVariant


@Suppress("DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION")
internal val DeprecatedAndroidBaseVariant.isTestVariant: Boolean
    get() = this is TestVariant || this is UnitTestVariant

internal fun Project.prepareAbiClasspath(): Configuration {
    val version = getKotlinPluginVersion()
    val dependency = dependencies.create("org.jetbrains.kotlin:abi-tools:$version")
    return configurations.detachedConfiguration(dependency)
}
