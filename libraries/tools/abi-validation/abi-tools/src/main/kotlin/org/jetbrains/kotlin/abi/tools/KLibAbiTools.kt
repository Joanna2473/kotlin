/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.KlibAbiSuit
import org.jetbrains.kotlin.abi.tools.api.KlibAbiToolsInterface
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbiReader
import java.io.File

@OptIn(ExperimentalLibraryAbiReader::class)
public class KLibAbiTools : KlibAbiToolsInterface {
    override fun dumpToLegacyFile(suites: List<KlibAbiSuit>, filters: AbiFilters, outputFile: File) {
        LibraryAbiReader.readAbiInfo()
    }
}
