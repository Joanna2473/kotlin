/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.klib

import org.jetbrains.kotlin.library.abi.*
import java.io.File
import java.io.FileNotFoundException

/**
 * Filters affecting how the klib ABI will be represented in a dump.
 */
internal class KlibDumpFilters internal constructor(
    /**
     * Names of packages that should be excluded from a dump.
     * If a package is listed here, none of its declarations will be included in a dump.
     */
    public val ignoredPackages: Set<String>,
    /**
     * Names of classes that should be excluded from a dump.
     */
    public val ignoredClasses: Set<String>,
    /**
     * Names of annotations marking non-public declarations.
     * Such declarations will be excluded from a dump.
     */
    public val nonPublicMarkers: Set<String>,
) {

    public class Builder @PublishedApi internal constructor() {
        /**
         * Names of packages that should be excluded from a dump.
         * If a package is listed here, none of its declarations will be included in a dump.
         *
         * By default, there are no ignored packages.
         */
        public val ignoredPackages: MutableSet<String> = mutableSetOf()

        /**
         * Names of classes that should be excluded from a dump.
         *
         * By default, there are no ignored classes.
         */
        public val ignoredClasses: MutableSet<String> = mutableSetOf()

        /**
         * Names of annotations marking non-public declarations.
         * Such declarations will be excluded from a dump.
         *
         * By default, a set of non-public markers is empty.
         */
        public val nonPublicMarkers: MutableSet<String> = mutableSetOf()

        @PublishedApi
        internal fun build(): KlibDumpFilters {
            return KlibDumpFilters(ignoredPackages, ignoredClasses, nonPublicMarkers)
        }
    }

    public companion object {
        /**
         * Default KLib ABI dump filters which declares no filters
         * and uses the latest KLib ABI signature version available.
         */
        public val DEFAULT: KlibDumpFilters = KlibDumpFilters {}
    }
}

/**
 * Builds a new [KlibDumpFilters] instance by invoking a [builderAction] on a temporary
 * [KlibDumpFilters.Builder] instance and then converting it into filters.
 *
 * Supplied [KlibDumpFilters.Builder] is valid only during the scope of [builderAction] execution.
 */
internal fun KlibDumpFilters(builderAction: KlibDumpFilters.Builder.() -> Unit): KlibDumpFilters {
    val builder = KlibDumpFilters.Builder()
    builderAction(builder)
    return builder.build()
}

@OptIn(ExperimentalLibraryAbiReader::class)
internal fun dumpTo(to: Appendable, klibFile: File, filters: KlibDumpFilters) {
    if (!klibFile.exists()) {
        throw FileNotFoundException("File does not exist: ${klibFile.absolutePath}")
    }
    val abiFilters = mutableListOf<AbiReadingFilter>()
    filters.ignoredClasses.toKlibNames().also {
        if (it.isNotEmpty()) {
            abiFilters.add(AbiReadingFilter.ExcludedClasses(it))
        }
    }
    filters.nonPublicMarkers.toKlibNames().also {
        if (it.isNotEmpty()) {
            abiFilters.add(AbiReadingFilter.NonPublicMarkerAnnotations(it))
        }
    }
    if (filters.ignoredPackages.isNotEmpty()) {
        abiFilters.add(AbiReadingFilter.ExcludedPackages(filters.ignoredPackages.map { AbiCompoundName(it) }))
    }

    val library = try {
        LibraryAbiReader.readAbiInfo(klibFile, abiFilters)
    } catch (t: Throwable) {
        throw IllegalStateException("Unable to read klib from ${klibFile.absolutePath}", t)
    }

    val supportedSignatureVersions = library.signatureVersions.asSequence().filter { it.isSupportedByAbiReader }

    val signatureVersion =
        supportedSignatureVersions.maxByOrNull { it.versionNumber } ?: throw IllegalStateException("Can't choose signatureVersion")

    LibraryAbiRenderer.render(
        library, to, AbiRenderingSettings(
            renderedSignatureVersion = signatureVersion,
            renderManifest = true,
            renderDeclarations = true
        )
    )
}

// We're assuming that all names are in valid binary form as it's described in JVMLS §13.1:
// https://docs.oracle.com/javase/specs/jls/se21/html/jls-13.html#jls-13.1
@OptIn(ExperimentalLibraryAbiReader::class)
private fun Collection<String>.toKlibNames(): List<AbiQualifiedName> =
    this.map(String::toAbiQualifiedName).filterNotNull()

@OptIn(ExperimentalLibraryAbiReader::class)
internal fun String.toAbiQualifiedName(): AbiQualifiedName? {
    if (this.isBlank() || this.contains('/')) return null
    // Easiest part: dissect package name from the class name
    val idx = this.lastIndexOf('.')
    if (idx == -1) {
        return AbiQualifiedName(AbiCompoundName(""), this.classNameToCompoundName())
    } else {
        val packageName = this.substring(0, idx)
        val className = this.substring(idx + 1)
        return AbiQualifiedName(AbiCompoundName(packageName), className.classNameToCompoundName())
    }
}

@OptIn(ExperimentalLibraryAbiReader::class)
private fun String.classNameToCompoundName(): AbiCompoundName {
    if (this.isEmpty()) return AbiCompoundName(this)

    val segments = mutableListOf<String>()
    val builder = StringBuilder()

    for (idx in this.indices) {
        val c = this[idx]
        // Don't treat a character as a separator if:
        // - it's not a '$'
        // - it's at the beginning of the segment
        // - it's the last character of the string
        if (c != '$' || builder.isEmpty() || idx == this.length - 1) {
            builder.append(c)
            continue
        }
        check(c == '$')
        // class$$$susbclass -> class.$$subclass, were at second $ here.
        if (builder.last() == '$') {
            builder.append(c)
            continue
        }

        segments.add(builder.toString())
        builder.clear()
    }
    if (builder.isNotEmpty()) {
        segments.add(builder.toString())
    }
    return AbiCompoundName(segments.joinToString(separator = "."))
}
