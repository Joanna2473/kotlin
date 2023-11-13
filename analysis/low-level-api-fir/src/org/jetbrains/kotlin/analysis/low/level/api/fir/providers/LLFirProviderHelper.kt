/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.getNotNullValueForNotNullContext
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.llResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirKotlinSymbolNamesProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.analysis.providers.impl.declarationProviders.CompositeKotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.impl.packageProviders.CompositeKotlinPackageProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallablePath
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry

internal class LLFirProviderHelper(
    firSession: LLFirSession,
    private val firFileBuilder: LLFirFileBuilder,
    canContainKotlinPackage: Boolean,
    declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?
) {
    private val extensionTool: LLFirResolveExtensionTool? = firSession.llResolveExtensionTool

    val searchScope: GlobalSearchScope =
        firSession.ktModule.contentScope.run {
            val notShadowedScope = extensionTool?.shadowedSearchScope?.let { GlobalSearchScope.notScope(it) }
            if (notShadowedScope != null) {
                this.intersectWith(notShadowedScope)
            } else {
                this
            }
        }

    val declarationProvider = CompositeKotlinDeclarationProvider.create(
        listOfNotNull(
            declarationProviderFactory(searchScope),
            extensionTool?.declarationProvider,
        )
    )

    val packageProvider = CompositeKotlinPackageProvider.create(
        listOfNotNull(
            firSession.project.createPackageProvider(searchScope),
            extensionTool?.packageProvider,
        )
    )

    val allowKotlinPackage: Boolean = canContainKotlinPackage ||
            firSession.languageVersionSettings.getFlag(AnalysisFlags.allowKotlinPackage)

    private val classifierByClassId =
        firSession.firCachesFactory.createCache<ClassId, FirClassLikeDeclaration?, KtClassLikeDeclaration?> { classId, context ->
            require(context == null || context.isPhysical)
            val ktClass = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return@createCache null

            if (ktClass.getClassId() == null) return@createCache null
            val firFile = firFileBuilder.buildRawFirFileWithCaching(ktClass.containingKtFile)
            FirElementFinder.findClassifierWithClassId(firFile, classId)
                ?: errorWithAttachment("Classifier was found in KtFile but was not found in FirFile") {
                    withEntry("classifierClassId", classId) { it.asString() }
                    withVirtualFileEntry("virtualFile", ktClass.containingKtFile.virtualFile)
                }
        }

    private val callablesByCallablePath =
        firSession.firCachesFactory.createCache<CallablePath, List<FirCallableSymbol<*>>, Collection<KtFile>?> { callableId, context ->
            require(context == null || context.all { it.isPhysical })
            val files = context ?: declarationProvider.getTopLevelCallableFiles(callableId).ifEmpty { return@createCache emptyList() }
            buildList {
                files.forEach { ktFile ->
                    val firFile = firFileBuilder.buildRawFirFileWithCaching(ktFile)
                    firFile.collectCallableDeclarationsTo(this, callableId.callableName)
                }
            }
        }

    val symbolNameCache = FirCompositeCachedSymbolNamesProvider.create(
        firSession,
        listOfNotNull(
            LLFirKotlinSymbolNamesProvider(declarationProvider, allowKotlinPackage),
            extensionTool?.symbolNamesProvider,
        )
    )

    fun getFirClassifierByFqNameAndDeclaration(
        classId: ClassId,
        classLikeDeclaration: KtClassLikeDeclaration?,
    ): FirClassLikeDeclaration? {
        if (classId.isLocal) return null
        if (!allowKotlinPackage && classId.isKotlinPackage()) return null
        return classifierByClassId.getNotNullValueForNotNullContext(classId, classLikeDeclaration)
    }

    fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        if (!allowKotlinPackage && packageFqName.isKotlinPackage()) return emptyList()
        val callablePath = CallablePath(packageFqName, name)
        return callablesByCallablePath.getValue(callablePath)
    }

    /**
     * [callableFiles] are the [KtFile]s which contain callables of the given package and name. If already known, they can be provided to
     * avoid index accesses.
     */
    fun getTopLevelCallableSymbols(callablePath: CallablePath, callableFiles: Collection<KtFile>?): List<FirCallableSymbol<*>> {
        if (!allowKotlinPackage && callablePath.packageName.isKotlinPackage()) return emptyList()
        return callablesByCallablePath.getValue(callablePath, callableFiles)
    }

    fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        return getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<FirNamedFunctionSymbol>()
    }

    fun getTopLevelFunctionSymbols(callablePath: CallablePath, callableFiles: Collection<KtFile>): List<FirNamedFunctionSymbol> {
        return getTopLevelCallableSymbols(callablePath, callableFiles).filterIsInstance<FirNamedFunctionSymbol>()
    }

    fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        return getTopLevelCallableSymbols(packageFqName, name).filterIsInstance<FirPropertySymbol>()
    }

    fun getTopLevelPropertySymbols(callablePath: CallablePath, callableFiles: Collection<KtFile>): List<FirPropertySymbol> {
        return getTopLevelCallableSymbols(callablePath, callableFiles).filterIsInstance<FirPropertySymbol>()
    }

    private fun FirFile.collectCallableDeclarationsTo(list: MutableList<FirCallableSymbol<*>>, name: Name) {
        declarations.mapNotNullTo(list) { declaration ->
            if (declaration is FirCallableDeclaration && declaration.symbol.callablePath.callableName == name) {
                declaration.symbol
            } else null
        }
    }

    fun getPackage(fqName: FqName): FqName? {
        if (!allowKotlinPackage && fqName.isKotlinPackage()) return null
        return fqName.takeIf(packageProvider::doesKotlinOnlyPackageExist)
    }
}

private fun ClassId.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
private fun FqName.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)