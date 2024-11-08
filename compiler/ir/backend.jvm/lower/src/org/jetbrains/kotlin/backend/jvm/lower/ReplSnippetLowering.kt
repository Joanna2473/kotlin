/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.name.NameUtils

@PhaseDescription(name = "ReplSnippetsToClasses")
internal class ReplSnippetsToClassesLowering(val context: JvmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val snippets = mutableListOf<IrReplSnippet>()

        for (irFile in irModule.files) {
            val iterator = irFile.declarations.listIterator()
            while (iterator.hasNext()) {
                val declaration = iterator.next()
                if (declaration is IrReplSnippet) {
                    val scriptClass = prepareReplSnippetClass(irFile, declaration)
                    snippets.add(declaration)
                    iterator.set(scriptClass)
                }
            }
        }

        val symbolRemapper = ReplSnippetsToClassesSymbolRemapper()

        snippets.sortBy { it.name }
//        for (irSnippet in snippets) {
//            finalizeReplSnippetClass(irSnippet, symbolRemapper)
//            // TODO fix parents in script classes
//            irSnippet.targetClass!!.owner.patchDeclarationParents(irSnippet.parent)
//        }
    }

    private fun prepareReplSnippetClass(irFile: IrFile, irSnippet: IrReplSnippet): IrClass {
        val fileEntry = irFile.fileEntry
        return context.irFactory.buildClass {
            startOffset = 0
            endOffset = fileEntry.maxOffset
            origin = IrDeclarationOrigin.REPL_SNIPPET_CLASS
            name = irSnippet.name.let {
                if (it.isSpecial) {
                    NameUtils.getScriptNameForFile(it.asStringStripSpecialMarkers().removePrefix("script-"))
                } else it
            }
            kind = ClassKind.OBJECT
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
        }.also { irSnippetClass ->
            irSnippetClass.superTypes += (context.irBuiltIns.anyNType)
            irSnippetClass.parent = irFile
            irSnippetClass.metadata = irSnippet.metadata
            irSnippet.targetClass = irSnippetClass.symbol
        }
    }
}

private class ReplSnippetsToClassesSymbolRemapper : SymbolRemapper.Empty() {
    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        (symbol.owner as? IrReplSnippet)?.targetClass  ?: symbol
}

