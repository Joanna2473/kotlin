/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine

internal val AbiValidationAction = KotlinProjectSetupCoroutine {
    val abiClasspath = prepareAbiClasspath()
    when {
        kotlinJvmExtensionOrNull != null -> abiValidationForKotlinJvm(abiClasspath)
        kotlinExtension is KotlinAndroidProjectExtension -> abiValidationForKotlinAndroid(abiClasspath)
        multiplatformExtensionOrNull != null -> abiValidationForKotlinMultiplatform(abiClasspath)
    }
}
