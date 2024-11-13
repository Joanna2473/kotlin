/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

import java.io.File

public interface JvmAbiToolsInterface {
    public fun dumpTo(outputFile: File, suites: List<JvmAbiSuit>, filters: AbiFilters)
}

public class JvmAbiSuit(
    public val name: String,
    public val classfiles: Iterable<File>,
)
