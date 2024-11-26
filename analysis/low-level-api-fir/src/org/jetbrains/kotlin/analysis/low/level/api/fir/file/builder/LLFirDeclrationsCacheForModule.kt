/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder

import com.google.common.collect.MapMaker
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.ThreadSafe
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry
import java.util.concurrent.ConcurrentMap

/**
 * Caches the [KtFile] to [FirFile] mapping of a [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule].
 */
@ThreadSafe
internal abstract class ModuleFileCache {
    abstract val moduleComponents: LLFirModuleResolveComponents

    /**
     * @return [FirFile] by [file] if it was previously built or runs [createValue] otherwise
     * The [createValue] is run under the lock so [createValue] is executed at most once for each [KtFile]
     */
    abstract fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile

    /**
     * @return [FirFile] for the [declaration]'s containing file, or `null` if declaration doesn't have a containing file.
     */
    abstract fun getContainerFirFile(declaration: FirDeclaration): FirFile?

    /**
     * @return cached [FirFile] for the [ktFile].
     */
    abstract fun getCachedFirFile(ktFile: KtFile): FirFile
}

internal class ModuleFileCacheImpl(override val moduleComponents: LLFirModuleResolveComponents) : ModuleFileCache() {
    private val ktFileToFirFile: ConcurrentMap<KtFile, FirFile> = MapMaker().weakKeys().makeMap()
    override fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile =
        ktFileToFirFile.computeIfAbsent(file) { createValue() }

    override fun getCachedFirFile(ktFile: KtFile): FirFile {
        return ktFileToFirFile[ktFile] ?: errorWithAttachment("FirFile was not found in cache for KtFile") {
            withVirtualFileEntry("ktFileName", ktFile.virtualFile)
            withEntry("session", moduleComponents.session.toString())
        }
    }

    override fun getContainerFirFile(declaration: FirDeclaration): FirFile? {
        val ktFile = declaration.psi?.containingFile as? KtFile ?: return null
        return getCachedFirFile(ktFile)
    }
}
