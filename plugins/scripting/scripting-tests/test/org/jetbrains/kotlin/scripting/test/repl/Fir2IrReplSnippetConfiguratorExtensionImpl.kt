/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.repl

import org.jetbrains.kotlin.backend.jvm.originalSnippetValueSymbol
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrReplSnippetConfiguratorExtension
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NameUtils
import kotlin.script.experimental.host.ScriptingHostConfiguration

class Fir2IrReplSnippetConfiguratorExtensionImpl(
    session: FirSession,
    // TODO: left here because it seems it will be needed soon, remove suppression if used or remove the param if it is not the case
    @Suppress("UNUSED_PARAMETER", "unused") hostConfiguration: ScriptingHostConfiguration,
) : Fir2IrReplSnippetConfiguratorExtension(session) {

    @OptIn(SymbolInternals::class)
    override fun Fir2IrComponents.prepareSnippet(firReplSnippet: FirReplSnippet, irSnippet: IrReplSnippet) {
        val propertiesFromState = hashSetOf<Pair<FirReplSnippetSymbol, FirPropertySymbol>>()
        val functionsFromState = hashSetOf<Pair<FirReplSnippetSymbol, FirNamedFunctionSymbol>>()
        val classesFromState = hashSetOf<Pair<FirReplSnippetSymbol, FirRegularClassSymbol>>()

        CollectAccessToOtherState(
            session,
            propertiesFromState,
            functionsFromState,
            classesFromState
        ).visitReplSnippet(firReplSnippet)

        val usedOtherSnippets = HashSet<FirReplSnippetSymbol>()
        propertiesFromState.mapTo(usedOtherSnippets) { it.first }
        functionsFromState.mapTo(usedOtherSnippets) { it.first }
        classesFromState.mapTo(usedOtherSnippets) { it.first }
        usedOtherSnippets.remove(firReplSnippet.symbol)
        usedOtherSnippets.forEach {
            val packageFragment = declarationStorage.getIrExternalPackageFragment(it.packageFqName(), it.moduleData)
            classifierStorage.createAndCacheEarlierSnippetClass(it, packageFragment)
        }

        propertiesFromState.forEach { (snippetSymbol, propertySymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                declarationStorage.createAndCacheIrVariable(
                    propertySymbol.fir, irSnippet, IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                ).also { varFromOtherSnippet ->
                    irSnippet.variablesFromOtherSnippets.add(varFromOtherSnippet)
                    val field = originalSnippet.addField {
                        name = varFromOtherSnippet.name
                        type = varFromOtherSnippet.type
                        visibility = DescriptorVisibilities.PUBLIC
                        origin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                    }
                    varFromOtherSnippet.originalSnippetValueSymbol = field.symbol
                }
            }
        }

        functionsFromState.forEach { (snippetSymbol, functionSymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                declarationStorage.createAndCacheIrFunction(
                    functionSymbol.fir,
                    originalSnippet,
                    predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET,
                    fakeOverrideOwnerLookupTag = null,
                    allowLazyDeclarationsCreation = true
                ).run {
                    parent = originalSnippet
                    visibility = DescriptorVisibilities.PUBLIC
                    dispatchReceiverParameter = IrFactoryImpl.createValueParameter(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = origin,
                        kind = null,
                        name = SpecialNames.THIS,
                        type = originalSnippet.thisReceiver!!.type,
                        isAssignable = false,
                        symbol = IrValueParameterSymbolImpl(),
                        varargElementType = null,
                        isCrossinline = false,
                        isNoinline = false,
                        isHidden = false,
                    ).apply {
                        parent = this@run
                    }
                    irSnippet.capturingDeclarationsFromOtherSnippets.add(this)
                }
            }
        }

        classesFromState.forEach { (snippetSymbol, classSymbol) ->
            classifierStorage.getCachedEarlierSnippetClass(snippetSymbol)?.let { originalSnippet ->
                classifierStorage.createAndCacheIrClass(
                    classSymbol.fir,
                    originalSnippet,
                    predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET,
                ).run {
                    parent = originalSnippet
                    visibility = DescriptorVisibilities.PUBLIC
                    createThisReceiverParameter()
                    val c = classSymbol.fir.primaryConstructorIfAny(session)?.let {
                        declarationStorage.createAndCacheIrConstructor(it.fir, { this }, isLocal = false)
                    }
                    classSymbol.fir.declarations.forEach { declaration ->
                        when (declaration) {
                            is FirProperty -> declarationStorage.createAndCacheIrProperty(
                                declaration,
                                this,
                                predefinedOrigin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                            )
                            else -> {}
                        }
                    }
                    irSnippet.capturingDeclarationsFromOtherSnippets.add(this)
                }
            }
        }
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> Fir2IrReplSnippetConfiguratorExtensionImpl(session, hostConfiguration) }
        }
    }
}

private fun FirReplSnippetSymbol.getTargetClassId(): ClassId {
    // TODO: either make this transformation here but configure/retain target script name somewhere, or abstract it away, or make it on lowering
    val snippetTargetName = NameUtils.getScriptNameForFile(name.asStringStripSpecialMarkers().removePrefix("script-"))
    // TODO: take base package from snippet symbol (see todo elsewhere for adding it to the symbol)
    return ClassId(FqName.ROOT, snippetTargetName)
}

private class CollectAccessToOtherState(
    val session: FirSession,
    val properties: MutableSet<Pair<FirReplSnippetSymbol, FirPropertySymbol>>,
    val functions: MutableSet<Pair<FirReplSnippetSymbol, FirNamedFunctionSymbol>>,
    val classes: MutableSet<Pair<FirReplSnippetSymbol, FirRegularClassSymbol>>,
) : FirDefaultVisitorVoid() {

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    @OptIn(SymbolInternals::class)
    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        val resolvedSymbol = resolvedNamedReference.resolvedSymbol
        val symbol = when (resolvedSymbol) {
            is FirConstructorSymbol -> (resolvedSymbol.fir.returnTypeRef as? FirResolvedTypeRef)?.coneType?.toSymbol(session)
            else -> null
        } ?: resolvedSymbol
        val originalSnippet = symbol.fir.originalReplSnippetSymbol ?: return
        when (symbol) {
            is FirPropertySymbol -> properties.add(originalSnippet to symbol)
            is FirNamedFunctionSymbol -> functions.add(originalSnippet to symbol)
            is FirRegularClassSymbol -> classes.add(originalSnippet to symbol)
            else -> {}
        }
    }
}