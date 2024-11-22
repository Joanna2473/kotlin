/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.scripting.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.INTERNAL
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.IrFunctionBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.name.SpecialNames

@PhaseDescription(name = "ReplSnippetsToClasses")
internal class ReplSnippetsToClassesLowering(val context: JvmBackendContext) : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        val snippets = mutableListOf<IrReplSnippet>()

        for (irFile in irModule.files) {
            val iterator = irFile.declarations.listIterator()
            while (iterator.hasNext()) {
                val declaration = iterator.next()
                if (declaration is IrReplSnippet) {
                    val snippetClass = prepareReplSnippetClass(irFile, declaration)
                    snippets.add(declaration)
                    iterator.set(snippetClass)
                }
            }
        }

        val symbolRemapper = ReplSnippetsToClassesSymbolRemapper()

        snippets.sortBy { it.name }
        for (irSnippet in snippets) {
            finalizeReplSnippetClass(irSnippet, symbolRemapper)
            irSnippet.targetClass!!.owner.patchDeclarationParents(irSnippet.parent)
        }
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

    private fun finalizeReplSnippetClass(irSnippet: IrReplSnippet, symbolRemapper: ReplSnippetsToClassesSymbolRemapper) {

        val irSnippetClass = irSnippet.targetClass!!.owner
        val typeRemapper = SimpleTypeRemapper(symbolRemapper)

        val implicitReceiversFieldsWithParameters = makeImplicitReceiversFieldsWithParameters(irSnippetClass, typeRemapper, irSnippet)

        val scriptTransformer = ReplSnippetToClassTransformer(
            irSnippet,
            irSnippetClass,
            typeRemapper,
            context,
            emptySet(),
            implicitReceiversFieldsWithParameters
        )
        val lambdaPatcher = ScriptFixLambdasTransformer(irSnippetClass)

        irSnippetClass.thisReceiver = scriptTransformer.targetClassReceiver
        irSnippetClass.declarations.add(createConstructor(irSnippetClass))

        fun <E : IrElement> E.patchStatementForClass(): IrElement {
            val rootContext =
                ScriptLikeToClassTransformerContext.makeRootContext(valueParameterForScriptThis = null, isInScriptConstructor = false)
            return transform(scriptTransformer, rootContext)
                .transform(lambdaPatcher, ScriptFixLambdasTransformerContext())
        }

        irSnippetClass.addFunction {
            name = Name.identifier("eval")
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = context.irBuiltIns.unitType // TODO: implement value returning
            visibility = INTERNAL
        }.apply {
            parent = irSnippetClass
            val mapClass = context.irBuiltIns.mutableMapClass
            val mapGet = mapClass.functions.single { it.owner.name.asString() == "get" }
            val mapPut = mapClass.functions.single { it.owner.name.asString() == "put" }
            val replStateParameter = context.irFactory.createValueParameter(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET,
                Name.special("<repl state>"),
                symbol = IrValueParameterSymbolImpl(),
                type = mapClass.typeWith(context.irBuiltIns.stringType, context.irBuiltIns.anyNType),
                varargElementType = null,
                isAssignable = false,
                isCrossinline = false,
                isHidden = false,
                isNoinline = false,
            ).also { parent = this }
            valueParameters = listOf(replStateParameter)
            var lastExpression: IrExpression? = null
            body =
                context.createIrBuilder(symbol).irBlockBody {
                    irSnippet.propertiesFromOtherSnippets.forEach {
                        it.initializer = irCall(mapGet).apply {
                            dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, replStateParameter.symbol)
                            putValueArgument(0, irString(it.name.asString()))
                        }
                        +(it.patchStatementForClass() as IrStatement)
                    }
                    val flattenedStatements = irSnippet.body.statements.flatMap { snippetStatement ->
                        if (snippetStatement is IrComposite) {
                            snippetStatement.statements
                        } else {
                            listOf(snippetStatement)
                        }
                    }
                    lastExpression = flattenedStatements.lastOrNull() as? IrExpression
                    flattenedStatements.forEach { statement ->
                        val patchedStatement = statement.patchStatementForClass() as IrStatement
                        if (statement == lastExpression) {
                            +irReturn(patchedStatement as IrExpression)
                        } else {
                            when (patchedStatement) {
                                is IrSimpleFunction,
                                is IrClass -> {
                                    patchedStatement.visibility = DescriptorVisibilities.PUBLIC
                                    irSnippetClass.declarations.add(patchedStatement)
                                }
                                is IrVariable -> {
                                    +patchedStatement
                                    +irCall(mapPut).apply {
                                        dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, replStateParameter.symbol)
                                        putValueArgument(0, irString(patchedStatement.name.asString()))
                                        putValueArgument(1, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, patchedStatement.symbol))
                                    }
                                }
                                else -> {
                                    +patchedStatement
                                }
                            }
                        }
                    }
                }
            returnType = lastExpression?.type ?: context.irBuiltIns.unitType
        }

        irSnippetClass.annotations += (irSnippetClass.parent as IrFile).annotations
    }

    private fun createConstructor(irSnippetClass: IrClass): IrConstructor =
        with(IrFunctionBuilder().apply {
            isPrimary = true
            returnType = irSnippetClass.thisReceiver!!.type as IrSimpleType
        }) {
            irSnippetClass.factory.createConstructor(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = origin,
                name = SpecialNames.INIT,
                visibility = visibility,
                isInline = isInline,
                isExpect = isExpect,
                returnType = returnType,
                symbol = IrConstructorSymbolImpl(),
                isPrimary = isPrimary,
                isExternal = isExternal,
                containerSource = containerSource,
            )
        }.also { irConstructor ->
            irConstructor.body = context.createIrBuilder(irConstructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
            }
            irConstructor.parent = irSnippetClass
        }
}

private class ReplSnippetsToClassesSymbolRemapper : SymbolRemapper.Empty() {
    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol =
        (symbol.owner as? IrReplSnippet)?.targetClass ?: symbol
}

internal class ReplSnippetToClassTransformer(
    val irSnippet: IrReplSnippet,
    irSnippetClass: IrClass,
    typeRemapper: TypeRemapper,
    context: JvmBackendContext,
    capturingClasses: Set<IrClassImpl>,
    implicitReceiversFieldsWithParameters: Collection<Pair<IrField, IrValueParameter>>
) : ScriptLikeToClassTransformer(
    irSnippet,
    irSnippetClass,
    typeRemapper,
    context,
    capturingClasses,
    implicitReceiversFieldsWithParameters,
    needsReceiverProcessing = true
) {
    override val targetClassReceiver: IrValueParameter = run {
        val type = IrSimpleTypeImpl(irSnippetClass.symbol, false, emptyList(), emptyList())
        irSnippet.createThisReceiverParameter(context, IrDeclarationOrigin.INSTANCE_RECEIVER, type)
            .transform(this, ScriptLikeToClassTransformerContext(null, null, null, false))
    }

    override fun visitSimpleFunction(
        declaration: IrSimpleFunction,
        data: ScriptLikeToClassTransformerContext,
    ): IrSimpleFunction {
        if (declaration.parent == irSnippet || declaration.parent == irTargetClass) {
            declaration.visibility = DescriptorVisibilities.PUBLIC
        }
        return super.visitSimpleFunction(declaration, data)
    }
}

private fun makeImplicitReceiversFieldsWithParameters(irSnippetClass: IrClass, typeRemapper: SimpleTypeRemapper, irSnippet: IrReplSnippet) =
    arrayListOf<Pair<IrField, IrValueParameter>>().apply {

        fun createField(name: Name, type: IrType): IrField {
            val field = irSnippetClass.factory.createField(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.SCRIPT_IMPLICIT_RECEIVER,
                name = name,
                visibility = DescriptorVisibilities.PRIVATE,
                symbol = IrFieldSymbolImpl(),
                type = typeRemapper.remapType(type),
                isFinal = true,
                isStatic = false,
                isExternal = false
            )
            field.parent = irSnippetClass
            irSnippetClass.declarations.add(field)
            return field
        }

        irSnippet.receiversParameters.forEach { param ->
            val typeName = param.type.classFqName?.shortName()?.identifierOrNullIfSpecial
            add(
                createField(
                    Name.identifier($$$"$$implicitReceiver_$$${typeName ?: param.index.toString()}"),
                    param.type
                ) to param
            )
        }
    }
