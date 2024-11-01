/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

import java.io.File

public interface KlibAbiToolsInterface {
    public fun dumpToLegacyFile(suites: List<KlibAbiSuit>, filters: AbiFilters, outputFile: File)

    public fun dumpToV2File(suites: List<KlibAbiSuit>, filters: AbiFilters, outputFile: File)
}

public class KlibAbiSuit(
    public val name: String,
    public val klibFiles: Sequence<File>,
)
