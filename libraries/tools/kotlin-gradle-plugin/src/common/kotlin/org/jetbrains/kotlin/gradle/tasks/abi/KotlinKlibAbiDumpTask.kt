/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.abi

import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.AbiToolsInterface
import org.jetbrains.kotlin.abi.tools.api.KlibTarget

internal abstract class KotlinKlibAbiDumpTask : AbiToolsTask(), KotlinAbiDumpTask {
    companion object {
        const val KLIB_TASK_NAME = "dumpKlibAbi"
    }

    @get:InputFiles // don't fail the task if file does not exist, instead print custom error message from verify()
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val referenceDump: RegularFileProperty

    @get:OutputFile
    abstract override val dumpFile: RegularFileProperty

    @get:Nested
    abstract val suits: MapProperty<String, Suit>

    @get:Input
    abstract val unsupportedSuites: MapProperty<String, String>

    @get:Input
    abstract val legacyFormat: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val includedClasses: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val excludedClasses: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val includedAnnotatedWith: SetProperty<String>

    @get:Input
    @get:Optional
    abstract val excludedAnnotatedWith: SetProperty<String>

    override fun runTools(tools: AbiToolsInterface) {
        val filters = AbiFilters(
            includedClasses.getOrElse(emptySet()),
            excludedClasses.getOrElse(emptySet()),
            includedAnnotatedWith.getOrElse(emptySet()),
            excludedAnnotatedWith.getOrElse(emptySet())
        )

        val mergedDump = tools.klib.emptyDump()
        suits.get().forEach { (_, entry) ->
            val dump = tools.klib.parseKlib(entry.klibFile.files.first(), filters)
            mergedDump.merge(dump)
        }

        val unsupported = unsupportedSuites.get()
        val referenceFile = referenceDump.get().asFile
        if (unsupported.isNotEmpty() && referenceFile.exists() && referenceFile.isFile) {
            val targets = unsupported.map { (suitName, targetName) -> KlibTarget(targetName, suitName) }
            mergedDump.partialMerge(tools.klib.parse(referenceFile), targets)
        }

        mergedDump.printToFile(dumpFile.get().asFile)
    }

    internal class Suit(
        @get:Input
        val targetName: String,

        @get:InputFiles
        @get:Optional
        @get:PathSensitive(PathSensitivity.RELATIVE)
        val klibFile: FileCollection,
    )
}
