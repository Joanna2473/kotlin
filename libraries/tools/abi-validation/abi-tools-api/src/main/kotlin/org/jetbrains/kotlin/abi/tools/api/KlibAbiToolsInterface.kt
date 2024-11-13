/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

import java.io.File
import java.io.FileNotFoundException

public interface KlibAbiToolsInterface {
    /**
     * Create empty dump without any declarations and targets.
     */
    public fun emptyDump(): KlibDump

    /**
     * Loads a dump from a textual form.
     *
     * @throws IllegalArgumentException if [dumpFile] is empty.
     * @throws IllegalArgumentException if [dumpFile] is not a file.
     * @throws FileNotFoundException if [dumpFile] does not exist.
     */
    public fun parse(dumpFile: File): KlibDump

    /**
     * Reads a dump from a textual form.
     *
     * @throws IllegalArgumentException if this dump and the provided [dump] shares same targets.
     * @throws IllegalArgumentException if the provided [dump] is empty.
     */
    public fun parse(dump: CharSequence): KlibDump

    /**
     * Dumps a public ABI of a klib represented by [klibFile] using [filters]
     * and returns a [KlibDump] representing it.
     *
     * To control which declarations are dumped, [filters] could be used. By default, no filters will be applied.
     *
     * @throws IllegalStateException if a klib could not be loaded from [klibFile].
     * @throws FileNotFoundException if [klibFile] does not exist.
     */
    public fun parseKlib(klibFile: File, filters: AbiFilters = AbiFilters.EMPTY): KlibDump
}
