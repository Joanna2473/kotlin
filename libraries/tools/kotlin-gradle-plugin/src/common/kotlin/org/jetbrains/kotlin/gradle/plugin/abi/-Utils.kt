/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.abi.tools.api.konanTargetNameMapping
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.DeprecatedAndroidBaseVariant
import org.jetbrains.kotlin.konan.target.HostManager

internal fun extractUnderlyingTarget(target: KotlinTarget): String {
    if (target is KotlinNativeTarget) {
        return konanTargetNameMapping[target.konanTarget.name]!!
    }
    return when (target.platformType) {
        KotlinPlatformType.js -> "js"
        KotlinPlatformType.wasm -> when ((target as KotlinJsIrTarget).wasmTargetType) {
            KotlinWasmTargetType.WASI -> "wasmWasi"
            KotlinWasmTargetType.JS -> "wasmJs"
            else -> throw IllegalStateException("Unreachable")
        }
        else -> throw IllegalArgumentException("Unsupported platform type: ${target.platformType}")
    }
}

internal fun targetIsSupported(target: KotlinTarget): Boolean {
    return when (target) {
        is KotlinNativeTarget -> HostManager().isEnabled(target.konanTarget)
        else -> true
    }
}

internal val KotlinTarget.emitsKlib: Boolean
    get() {
        val platformType = this.platformType
        return platformType == KotlinPlatformType.native ||
                platformType == KotlinPlatformType.wasm ||
                platformType == KotlinPlatformType.js
    }

@Suppress("DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION")
internal val DeprecatedAndroidBaseVariant.isTestVariant: Boolean
    get() = this is TestVariant || this is UnitTestVariant

internal fun Project.prepareAbiClasspath(): Configuration {
    val version = getKotlinPluginVersion()
    val tools = dependencies.create("org.jetbrains.kotlin:abi-tools:$version")
    return configurations.detachedConfiguration(tools)
}
