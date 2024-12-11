/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.repl

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyCopy
import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.extensions.FirReplHistoryProvider
import org.jetbrains.kotlin.fir.extensions.FirReplSnippetResolveExtension
import org.jetbrains.kotlin.fir.extensions.replHistoryProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.resolve.FirReplHistoryScope
import kotlin.script.experimental.host.ScriptingHostConfiguration

class FirTestReplSnippetResolveExtensionImpl(
    session: FirSession,
    // TODO: left here because it seems it will be needed soon, remove suppression if used or remove the param if it is not the case
    @Suppress("UNUSED_PARAMETER", "unused") hostConfiguration: ScriptingHostConfiguration,
) : FirReplSnippetResolveExtension(session) {

    private val replHistoryProvider: FirReplHistoryProvider by lazy {
        session.moduleData.dependencies.firstOrNull()?.session?.replHistoryProvider ?: error("No repl history provider found")
    }

    @OptIn(SymbolInternals::class)
    override fun getSnippetScope(currentSnippet: FirReplSnippet, useSiteSession: FirSession): FirScope? {
        // TODO: consider caching (KT-72975)
        val properties = HashMap<Name, FirVariableSymbol<*>>()
        val functions = HashMap<Name, ArrayList<FirNamedFunctionSymbol>>() // TODO: find out how overloads should work
        val classLikes = HashMap<Name, FirClassLikeSymbol<*>>()
        replHistoryProvider.getSnippets().forEach { snippet ->
            if (currentSnippet == snippet) return@forEach
            snippet.fir.body.statements.forEach {
                if (it is FirDeclaration) {
                    it.originalReplSnippetSymbol = snippet
                    when (it) {
                        is FirProperty -> properties.put(it.name, it.createCopyForState(snippet).symbol)
                        is FirSimpleFunction -> functions.getOrPut(it.name, { ArrayList() }).add(it.symbol)
                        is FirRegularClass -> classLikes.put(it.name, it.symbol)
                        is FirTypeAlias -> classLikes.put(it.name, it.symbol)
                    }
                }
            }
        }
        return FirReplHistoryScope(properties, functions, classLikes, useSiteSession)
    }

    override fun updateResolved(snippet: FirReplSnippet) {
        replHistoryProvider.putSnippet(snippet.symbol)
    }

    private fun FirProperty.createCopyForState(snippet: FirReplSnippetSymbol): FirProperty {
        return buildPropertyCopy(this) {
            origin = FirDeclarationOrigin.FromOtherReplSnippet
            status = this@createCopyForState.status.copy(visibility = Visibilities.Local, isStatic = true)
            this.symbol = FirPropertySymbol(this@createCopyForState.symbol.callableId)
        }.also {
            it.originalReplSnippetSymbol = snippet
        }
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirTestReplSnippetResolveExtensionImpl(session, hostConfiguration) }
        }
    }
}