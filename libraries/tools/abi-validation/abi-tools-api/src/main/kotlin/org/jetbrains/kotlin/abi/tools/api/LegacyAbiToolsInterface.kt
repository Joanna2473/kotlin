/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

import java.io.File
import java.io.FileNotFoundException

/**
 * A set of features for working with legacy format dumps,
 * used in previos [Binary Compatibility Validator plugin](https://github.com/Kotlin/binary-compatibility-validator).
 *
 * @since 2.1.20
 */
public interface LegacyAbiToolsInterface {
    public fun <T : Appendable> dumpJvm(appendable: T, classfiles: Iterable<File>, filters: AbiFilters)

    /**
     * Create empty klib dump without any declarations and targets.
     */
    public fun emptyKlibDump(): KlibDump

    /**
     * Loads a klib dump from a dump file.
     *
     * @throws IllegalArgumentException if [dumpFile] is empty.
     * @throws IllegalArgumentException if [dumpFile] is not a file.
     * @throws FileNotFoundException if [dumpFile] does not exist.
     */
    public fun parseKlibDump(dumpFile: File): KlibDump

    /**
     * Reads a dump from a textual form.
     *
     * @throws IllegalArgumentException if this dump and the provided [dump] shares same targets.
     * @throws IllegalArgumentException if the provided [dump] is empty.
     */
    public fun parseKlibDump(dump: CharSequence): KlibDump

    /**
     * Reads a dump from a klib file.
     *
     * To control which declarations are passed to the dump, [filters] could be used. By default, no filters will be applied.
     *
     * @throws IllegalStateException if a klib could not be loaded from [klibFile].
     * @throws FileNotFoundException if [klibFile] does not exist.
     */
    public fun parseKlib(klibFile: File, filters: AbiFilters = AbiFilters.EMPTY): KlibDump

    /**
     * Compare two files line-by-line.
     *
     * @return `null` if there are no differences, diff string otherwise.
     *
     * @throws FileNotFoundException if [expectedFile] and/or [actualFile] does not exist.
     */
    public fun filesDiff(expectedFile: File, actualFile: File): String?
}
