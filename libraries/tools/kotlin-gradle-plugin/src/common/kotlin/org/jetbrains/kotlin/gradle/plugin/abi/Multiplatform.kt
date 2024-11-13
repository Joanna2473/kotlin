/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

internal suspend fun Project.abiValidationForKotlinMultiplatform(abiClasspath: Configuration) {
    val multiplatformExtension = multiplatformExtension
    val kotlinTargets = multiplatformExtension.awaitTargets()

    // wait until all compilations are configured
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    processJvmKindTargets(kotlinTargets, multiplatformExtension, abiClasspath)
    processNonJvmTargets(kotlinTargets, multiplatformExtension, abiClasspath)
}


private fun Project.processJvmKindTargets(
    targets: Iterable<KotlinTarget>,
    kotlinExtension: KotlinMultiplatformExtension,
    abiClasspath: Configuration,
) {
    val jvmTaskSet = jvmTaskSet(kotlinExtension.abiValidation, abiClasspath)
    targets
        .asSequence()
        .filter { target -> target.platformType == KotlinPlatformType.jvm }
        .forEach { target ->
            target.compilations.all { compilation ->
                // TODO additional source sets!
                if (compilation.name == SourceSet.MAIN_SOURCE_SET_NAME) {
                    jvmTaskSet.addSuit(target.targetName, compilation.output.classesDirs)
                }
            }
        }

    targets
        .asSequence()
        .filter { target -> target.platformType == KotlinPlatformType.androidJvm }
        .filterIsInstance<KotlinAndroidTarget>()
        .forEach { target ->
            target.compilations.all { compilation ->
                if (!compilation.androidVariant.isTestVariant) {
                    jvmTaskSet.addSuit(compilation.androidVariant.name, compilation.output.classesDirs)
                }
            }
        }
}


private fun Project.processNonJvmTargets(
    targets: Iterable<KotlinTarget>,
    kotlinExtension: KotlinMultiplatformExtension,
    abiClasspath: Configuration,
) {
    val taskSet = klibTaskSet(kotlinExtension.abiValidation, abiClasspath)
    targets
        .asSequence()
        .filter { target -> target.emitsKlib }
        .forEach { target ->
            val canonicalTargetName = extractUnderlyingTarget(target)

            if (targetIsSupported(target)) {
                target.compilations.all { compilation ->
                    // TODO additional source sets!
                    if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                        taskSet.addSuit(target.targetName, canonicalTargetName, compilation.output.classesDirs)
                    }
                }
            } else {
                taskSet.keepSuit(target.targetName, canonicalTargetName)
            }
        }
}
