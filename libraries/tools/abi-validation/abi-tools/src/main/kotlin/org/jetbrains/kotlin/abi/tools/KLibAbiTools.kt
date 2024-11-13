/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.KlibDump
import org.jetbrains.kotlin.abi.tools.api.KlibAbiToolsInterface
import org.jetbrains.kotlin.abi.tools.klib.KlibDumpImpl
import java.io.File

public class KLibAbiTools : KlibAbiToolsInterface {
    override fun emptyDump(): KlibDump {
        return KlibDumpImpl()
    }

    override fun parse(dumpFile: File): KlibDump {
        return KlibDumpImpl.from(dumpFile)
    }

    override fun parse(dump: CharSequence): KlibDump {
        return KlibDumpImpl.from(dump)
    }

    override fun parseKlib(klibFile: File, filters: AbiFilters): KlibDump {
        return KlibDumpImpl.fromKlib(klibFile, filters)
    }
}
