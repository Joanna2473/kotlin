/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

import java.io.File
import java.io.FileNotFoundException

/**
 * Represents KLib ABI dump and allows manipulating it.
 *
 * Usual [KlibDump] workflows consists of loading, updating and writing a dump back.
 *
 * **Creating a textual dump from a klib**
 * ```kotlin
 * val dump = KlibDump.fromKlib(File("/path/to/library.klib"))
 * dump.saveTo(File("/path/to/dump.klib.api"))
 * ```
 *
 * **Loading a dump**
 * ```kotlin
 * val dump = KlibDump.from(File("/path/to/dump.klib.api"))
 * ```
 *
 * **Merging multiple dumps into a new merged dump**
 * ```kotlin
 * val klibs = listOf(File("/path/to/library-linuxX64.klib"), File("/path/to/library-linuxArm64.klib"), ...)
 * val mergedDump = KlibDump()
 * klibs.forEach { mergedDump.mergeFromKlib(it) }
 * mergedDump.saveTo(File("/path/to/merged.klib.api"))
 * ```
 *
 * **Updating an existing merged dump**
 * ```kotlin
 * val mergedDump = KlibDump.from(File("/path/to/merged.klib.api"))
 * val newTargetDump = KlibDump.fromKlib(File("/path/to/library-linuxX64.klib"))
 * mergedDump.replace(newTargetDump)
 * mergedDump.saveTo(File("/path/to/merged.klib.api"))
 * ```
 */
public interface KlibDump {
    /**
     * Set of all targets for which this dump contains declarations.
     */
    public val targets: Set<KlibTarget>

    /**
     * Loads a textual KLib dump and merges it into this dump.
     *
     * It's an error to merge dumps having some targets in common.
     *
     * @throws IllegalArgumentException if this dump and [dumpFile] shares same targets.
     * @throws IllegalArgumentException if [dumpFile] is not a file or is empty.
     * @throws FileNotFoundException if [dumpFile] does not exist.
     */
    public fun merge(dumpFile: File)

    /**
     * Reads a textual KLib dump from the [dump] char sequence and merges it into this dump.
     *
     * It's also an error to merge dumps having some targets in common.
     *
     * @throws IllegalArgumentException if this dump and the provided [dump] shares same targets.
     * @throws IllegalArgumentException if the provided [dump] is empty.
     */
    public fun merge(dump: CharSequence)

    /**
     * Merges [other] dump with this one.
     *
     * It's also an error to merge dumps having some targets in common.
     *
     * The operation does not modify [other].
     *
     * @throws IllegalArgumentException if this dump and [other] shares same targets.
     */
    public fun merge(other: KlibDump)

    /**
     * Removes the targets from this dump that are contained in the [other] targets set and all their declarations.
     * Then merges the [other] dump with this one.
     *
     * The operation does not modify [other].
     */
    public fun replace(other: KlibDump)

    /**
     * Removes all declarations that do not belong to specified targets and removes these targets from the dump.
     *
     * All targets in the [targets] collection not contained within this dump will be ignored.
     */
    public fun retain(targets: Iterable<KlibTarget>)

    /**
     * Remove all declarations that do belong to specified targets and remove these targets from the dump.
     *
     * All targets in the [targets] collection not contained within this dump will be ignored.
     */
    public fun remove(targets: Iterable<KlibTarget>)

    /**
     * @throws IllegalStateException if dump contains multiple targets
     */
    public fun renameSingleTarget(target: KlibTarget)

    /**
     * Creates a copy of this dump.
     */
    public fun copy(): KlibDump

    /**
     * Serializes the dump and writes it to [to].
     *
     * @return the target [to] where the dump was written.
     */
    public fun <A : Appendable> printTo(to: A): A

    /**
     * Serializes the dump and writes it to [file].
     *
     * @return the target [file].
     */
    public fun printToFile(file: File): File

    public fun partialMerge(other: KlibDump, targetsFromOther: List<KlibTarget>)
}
