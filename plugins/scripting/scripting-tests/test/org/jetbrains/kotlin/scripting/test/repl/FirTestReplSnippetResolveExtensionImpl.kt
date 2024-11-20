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
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClassCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.extensions.FirReplHistoryProvider
import org.jetbrains.kotlin.fir.extensions.FirReplSnippetResolveExtension
import org.jetbrains.kotlin.fir.extensions.replHistoryProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.scripting.resolve.FirReplHistoryScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
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
                when (it) {
                    is FirProperty -> properties.put(it.name, it.createCopyForState(snippet).symbol)
                    is FirSimpleFunction -> functions.getOrPut(it.name, { ArrayList() }).add(it.createCopyForState(snippet).symbol)
                    is FirRegularClass -> classLikes.put(it.name, it.createCopyForState(snippet).symbol)
                    is FirTypeAlias -> classLikes.put(it.name, it.symbol)
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

    private fun FirReplSnippetSymbol.getTargetClassId(): ClassId {
        // TODO: either make this transformation here but configure/retain target script name somewhere, or abstract it away, or make it on lowering
        val snippetTargetName = NameUtils.getScriptNameForFile(name.asStringStripSpecialMarkers().removePrefix("script-"))
        // TODO: take base package from snippet symbol (see todo elsewhere for adding it to the symbol)
        return ClassId(FqName.ROOT, snippetTargetName)
    }

    private fun FirSimpleFunction.createCopyForState(snippet: FirReplSnippetSymbol): FirSimpleFunction {
        return buildSimpleFunctionCopy(this) {
            origin = FirDeclarationOrigin.FromOtherReplSnippet
            status = this@createCopyForState.status.copy(visibility = Visibilities.Public, isStatic = true)
            this.symbol = FirNamedFunctionSymbol(CallableId(snippet.getTargetClassId(), this@createCopyForState.symbol.callableId.callableName))
        }.also {
            it.originalReplSnippetSymbol = snippet
        }
    }

    private fun FirRegularClass.createCopyForState(snippet: FirReplSnippetSymbol): FirRegularClass {
        return buildRegularClassCopy(this) {
            status = this@createCopyForState.status.copy(visibility = Visibilities.Public, isStatic = true)
            this.symbol = FirRegularClassSymbol(snippet.getTargetClassId().createNestedClassId(name))
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