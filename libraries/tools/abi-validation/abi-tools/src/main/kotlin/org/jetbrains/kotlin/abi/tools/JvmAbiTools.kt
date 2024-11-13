/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.JvmAbiSuit
import org.jetbrains.kotlin.abi.tools.api.JvmAbiToolsInterface
import java.io.File

public class JvmAbiTools : JvmAbiToolsInterface {

    override fun dumpTo(outputFile: File, suites: List<JvmAbiSuit>, filters: AbiFilters) {
        val signatures = suites.asSequence()
            .flatMap { suite -> suite.classfiles }
            .map { classFile -> classFile.inputStream() }
            .loadApiFromJvmClasses()
            .filter(filters)

        outputFile.bufferedWriter().use { writer ->
            signatures.dump(writer)
        }
    }


}
