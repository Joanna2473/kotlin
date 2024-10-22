/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.abi.tools.api.KlibTarget
import org.jetbrains.kotlin.abi.tools.api.konanTargetNameMapping
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.DeprecatedAndroidBaseVariant
import org.jetbrains.kotlin.konan.target.HostManager

/**
 * Converts [KotlinTarget] to a [KlibTarget].
 */
public fun KotlinTarget.toKlibTarget(): KlibTarget = KlibTarget(extractUnderlyingTarget(this), targetName)

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

/**
 * Check specified target is supported by host compiler.
 */
internal fun targetIsSupported(target: KotlinTarget): Boolean {
    return when (target) {
        is KotlinNativeTarget -> HostManager().isEnabled(target.konanTarget)
        else -> true
    }
}

/**
 * Check specified target has klib file as output artifact.
 */
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

/**
 * Execute given [action] against compilation with name [SourceSet.MAIN_SOURCE_SET_NAME].
 */
internal inline fun <T : KotlinCompilation<*>> DomainObjectCollection<T>.withMainCompilation(crossinline action: T.() -> Unit) {
    all { compilation ->
        if (compilation.name == SourceSet.MAIN_SOURCE_SET_NAME) compilation.action()
    }
}