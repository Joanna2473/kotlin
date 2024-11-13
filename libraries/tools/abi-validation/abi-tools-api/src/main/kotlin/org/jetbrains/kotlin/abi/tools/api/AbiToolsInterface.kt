/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

import java.io.File

interface AbiToolsInterface {
    val jvm: JvmAbiToolsInterface
    val klib: KlibAbiToolsInterface

    /**
     * ! files should exist !
     */
    public fun filesDiff(expectedFile: File, actualFile: File): String?
}