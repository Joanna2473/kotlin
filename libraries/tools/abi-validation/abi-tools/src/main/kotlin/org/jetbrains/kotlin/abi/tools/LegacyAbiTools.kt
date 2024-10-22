/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.KlibDump
import org.jetbrains.kotlin.abi.tools.api.LegacyAbiToolsInterface
import org.jetbrains.kotlin.abi.tools.filtering.compileMatcher
import org.jetbrains.kotlin.abi.tools.klib.KlibDumpImpl
import java.io.File

public class LegacyAbiTools : LegacyAbiToolsInterface {

    override fun <T : Appendable> dumpJvm(appendable: T, classfiles: Iterable<File>, filters: AbiFilters) {
        val filtersMatcher = compileMatcher(filters)

        val signatures = classfiles.asSequence()
            .map { classfile -> classfile.inputStream() }
            .loadApiFromJvmClasses()
            .filterByMatcher(filtersMatcher)

        signatures.dump(appendable)
    }

    override fun emptyKlibDump(): KlibDump {
        return KlibDumpImpl()
    }

    override fun parseKlibDump(dumpFile: File): KlibDump {
        return KlibDumpImpl.from(dumpFile)
    }

    override fun parseKlibDump(dump: CharSequence): KlibDump {
        return KlibDumpImpl.from(dump)
    }

    override fun parseKlib(klibFile: File, filters: AbiFilters): KlibDump {
        return KlibDumpImpl.fromKlib(klibFile, filters)
    }

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
