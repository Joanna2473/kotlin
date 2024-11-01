/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.abi

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.utils.newInstance

internal fun Project.abiValidationMultiplatformDefault(): AbiValidationMultiplatformExtensionImpl {
    return objects.newInstance<AbiValidationMultiplatformExtensionImpl>(this)
        .configureAbiValidation(this)
}

internal fun Project.abiValidationJvmOrAndroidDefault(): AbiValidationJvmExtensionImpl {
    return objects.newInstance<AbiValidationJvmExtensionImpl>(this)
        .configureAbiValidation(this)
}

private const val DEFAULT_REFERENCE_DIR_PATH = "abi"
private const val DEFAULT_REFERENCE_DUMP_NAME = "jvm.abi"

private fun AbiValidationMultiplatformExtensionImpl.configureAbiValidation(project: Project): AbiValidationMultiplatformExtensionImpl {
    referenceDumpDir.convention(project.layout.projectDirectory.dir(DEFAULT_REFERENCE_DIR_PATH))
    jvm.referenceDumpFileName.convention(DEFAULT_REFERENCE_DUMP_NAME)
    useLegacyFormat.convention(false)
    return this
}

private fun AbiValidationJvmExtensionImpl.configureAbiValidation(project: Project): AbiValidationJvmExtensionImpl {
    referenceDumpDir.convention(project.layout.projectDirectory.dir(DEFAULT_REFERENCE_DIR_PATH))
    referenceDumpFileName.convention(DEFAULT_REFERENCE_DUMP_NAME)
    useLegacyFormat.convention(false)
    return this
}
