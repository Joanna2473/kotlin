/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.repl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import kotlin.script.experimental.host.ScriptingHostConfiguration

class Fir2IrReplSnippetConfiguratorExtensionImpl(
    session: FirSession,
    // TODO: left here because it seems it will be needed soon, remove suppression if used or remove the param if it is not the case
    @Suppress("UNUSED_PARAMETER", "unused") hostConfiguration: ScriptingHostConfiguration,
) : Fir2IrReplSnippetConfiguratorExtension(session) {

    @OptIn(SymbolInternals::class)
    override fun Fir2IrComponents.prepareSnippet(firReplSnippet: FirReplSnippet, irSnippet: IrReplSnippet) {
        val propertiesFromOtherSnippets = mutableListOf<FirPropertySymbol>()
        val functionsFromOtherSnippets = mutableListOf<Pair<FirReplSnippetSymbol, FirNamedFunctionSymbol>>()
        val classesFromOtherSnippets = mutableListOf<Pair<FirReplSnippetSymbol, FirRegularClassSymbol>>()

        CollectAccessToOtherSnippets(propertiesFromOtherSnippets, functionsFromOtherSnippets, classesFromOtherSnippets).visitReplSnippet(firReplSnippet)

        propertiesFromOtherSnippets.forEach { firPropertySymbol ->
            irSnippet.propertiesFromOtherSnippets.add(
                declarationStorage.createAndCacheIrVariable(
                    firPropertySymbol.fir, irSnippet,
                    givenOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                )
            )
        }
        val usedOtherSnippets = HashSet<FirReplSnippetSymbol>()
        functionsFromOtherSnippets.mapTo(usedOtherSnippets) { it.first }
        classesFromOtherSnippets.mapTo(usedOtherSnippets) { it.first }
        usedOtherSnippets.forEach {
            val packageFragment = declarationStorage.getIrExternalPackageFragment(it.packageFqName(), it.moduleData)
            classifierStorage.createAndCacheEarlierSnippetClass(it, packageFragment)
        }

        functionsFromOtherSnippets.forEach { (snippetSymbol, functionSymbol) ->
            val originalSnippet = classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)
            declarationStorage.createAndCacheIrFunction(
                functionSymbol.fir,
                originalSnippet,
                predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET,
                isLocal = true, // TODO: a hack to create regular IrFunction, rather that Fir2IrLazy* one
                fakeOverrideOwnerLookupTag = null,
                allowLazyDeclarationsCreation = true
            )
        }

        classesFromOtherSnippets.forEach { (snippetSymbol, classSymbol) ->
            val originalSnippet = classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)
            classifierStorage.createAndCacheIrClass(
                classSymbol.fir,
                originalSnippet!!,
                predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
            )
        }
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> Fir2IrReplSnippetConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}

private class CollectAccessToOtherSnippets(
    val properties: MutableList<FirPropertySymbol>,
    val functionsFromOtherSnippets: MutableList<Pair<FirReplSnippetSymbol, FirNamedFunctionSymbol>>,
    val classesFromOtherSnippets: MutableList<Pair<FirReplSnippetSymbol, FirRegularClassSymbol>> = mutableListOf()
) : FirDefaultVisitorVoid() {

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    @OptIn(SymbolInternals::class)
    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        val symbol = resolvedNamedReference.resolvedSymbol
        val originalSnippet = symbol.fir.originalReplSnippetSymbol
        when {
            originalSnippet == null -> {}
            symbol is FirPropertySymbol -> properties.add(symbol)
            symbol is FirNamedFunctionSymbol ->  functionsFromOtherSnippets.add(originalSnippet to symbol)
            symbol is FirRegularClassSymbol -> classesFromOtherSnippets.add(originalSnippet to symbol)
            else -> {}
        }
    }
}