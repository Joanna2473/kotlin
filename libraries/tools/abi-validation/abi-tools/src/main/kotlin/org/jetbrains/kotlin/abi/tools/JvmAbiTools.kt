/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.JvmAbiSuit
import org.jetbrains.kotlin.abi.tools.api.JvmAbiToolsInterface
import java.io.File

public class JvmAbiTools : JvmAbiToolsInterface {

    override fun dumpToLegacyFile(suites: List<JvmAbiSuit>, filters: AbiFilters, outputFile: File) {
        val signatures = suites.asSequence()
            .flatMap { suite -> suite.classfiles }
            .map { classFile -> classFile.inputStream() }
            .loadApiFromJvmClasses()
            .filter(filters)

        outputFile.bufferedWriter().use { writer ->
            signatures.dump(writer)
        }
    }

    /**
     * ! files should exist !
     */
    override fun filesDiff(
        expectedFile: File,
        actualFile: File,
    ): String? {
        val expectedText = expectedFile.readText()
        val actualText = actualFile.readText()

        // We don't compare a full text because newlines on Windows & Linux/macOS are different
        val expectedLines = expectedText.lines()
        val actualLines = actualText.lines()
        if (expectedLines == actualLines) {
            return null
        }

        val patch = DiffUtils.diff(expectedLines, actualLines)
        val diff =
            UnifiedDiffUtils.generateUnifiedDiff(expectedFile.toString(), actualFile.toString(), expectedLines, patch, 3)
        return diff.joinToString("\n")
    }
}
