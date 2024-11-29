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
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
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
            kind = ClassKind.CLASS
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

        val valsToFields = mutableMapOf<IrVariableSymbol, IrFieldSymbol>()

        val irSnippetClassType = IrSimpleTypeImpl(irSnippetClass.symbol, false, emptyList(), emptyList())
        val irSnippetClassThisReceiver = irSnippet.createThisReceiverParameter(context, IrDeclarationOrigin.INSTANCE_RECEIVER, irSnippetClassType)
        irSnippetClass.thisReceiver = irSnippetClassThisReceiver

        val stateField = irSnippetClass.addField {
            name = Name.special("<repl state>");
            type = context.irBuiltIns.mutableMapClass.typeWith(context.irBuiltIns.stringType, context.irBuiltIns.anyNType)
        }
        irSnippetClass.declarations.add(createConstructor(irSnippetClass, stateField))

        irSnippetClass.addFunction {
            name = Name.identifier("eval")
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = context.irBuiltIns.unitType // TODO: implement value returning
            visibility = INTERNAL
        }.apply {
            parent = irSnippetClass
            dispatchReceiverParameter = buildReceiverParameter(this, irSnippetClass.origin, irSnippetClass.defaultType, startOffset, endOffset)
            var lastExpression: IrExpression? = null
            body =
                context.createIrBuilder(symbol).irBlockBody {
                    val flattenedStatements = irSnippet.body.statements.flatMap { snippetStatement ->
                        if (snippetStatement is IrComposite) {
                            snippetStatement.statements
                        } else {
                            listOf(snippetStatement)
                        }
                    }
                    lastExpression = flattenedStatements.lastOrNull() as? IrExpression
                    var lastExpressionVar: IrVariable? = null
                    flattenedStatements.forEach { statement ->
                        if (statement == lastExpression) {
                            // Could become a `$res..` one
                            createTmpVariable(statement as IrExpression)
                        } else {
                            when (statement) {
                                is IrVariable -> {
                                    val prop = irSnippetClass.addField {
                                        startOffset = statement.startOffset
                                        endOffset= statement.endOffset
                                        name = statement.name
                                        type = statement.type
                                        visibility = DescriptorVisibilities.PUBLIC
                                        origin = IrDeclarationOrigin.DEFINED
                                        modality = Modality.FINAL
                                    }
                                    valsToFields[statement.symbol] = prop.symbol
                                }
                                is IrProperty,
                                is IrSimpleFunction,
                                is IrClass -> {
                                    statement.visibility = DescriptorVisibilities.PUBLIC
                                    irSnippetClass.declarations.add(statement)
                                }
                                else -> {
                                    +statement
                                }
                            }
                        }
                    }
                    // TODO: find why it fails now, and fix
//                    lastExpression?.let {
//                        +irReturn(IrGetValueImpl(it.startOffset, it.endOffset, lastExpressionVar!!.symbol))
//                    }
                }
            returnType = lastExpression?.type ?: context.irBuiltIns.unitType
        }

        val scriptTransformer = ReplSnippetToClassTransformer(
            irSnippet,
            irSnippetClassThisReceiver,
            irSnippetClass,
            typeRemapper,
            context,
            emptySet(),
            implicitReceiversFieldsWithParameters,
            stateField,
            valsToFields
        )
        val lambdaPatcher = ScriptFixLambdasTransformer(irSnippetClass)

        irSnippetClass.declarations.transformInPlace {
            val rootContext =
                if (it is IrConstructor)
                    ScriptLikeToClassTransformerContext.makeRootContext(irSnippetClass.thisReceiver!!.symbol, true)
                else
                    ScriptLikeToClassTransformerContext.makeRootContext(null, isInScriptConstructor = false, topLevelDeclaration = it)
            it.transform(scriptTransformer, rootContext)
                .transform(lambdaPatcher, ScriptFixLambdasTransformerContext())
        }

        irSnippetClass.annotations += (irSnippetClass.parent as IrFile).annotations
    }

    private fun createConstructor(irSnippetClass: IrClass, stateField: IrField): IrConstructor =
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
            irConstructor.addValueParameter {
                name = Name.special("<repl state>")
                origin = IrDeclarationOrigin.REPL_FROM_OTHER_SNIPPET
                type = stateField.type
            }
            irConstructor.body = context.createIrBuilder(irConstructor.symbol).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                +irSetField(irGet(irSnippetClass.thisReceiver!!), stateField, irGet(irConstructor.parameters[0]))
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
    override val targetClassReceiver: IrValueParameter,
    irSnippetClass: IrClass,
    typeRemapper: TypeRemapper,
    context: JvmBackendContext,
    capturingClasses: Set<IrClassImpl>,
    implicitReceiversFieldsWithParameters: Collection<Pair<IrField, IrValueParameter>>,
    val stateField: IrField,
    val varsToFields: Map<IrVariableSymbol, IrFieldSymbol>,
) : ScriptLikeToClassTransformer(
    irSnippet,
    irSnippetClass,
    typeRemapper,
    context,
    capturingClasses,
    implicitReceiversFieldsWithParameters,
    needsReceiverProcessing = true
) {
//    override val targetClassReceiver: IrValueParameter = run {
//        val type = IrSimpleTypeImpl(irSnippetClass.symbol, false, emptyList(), emptyList())
//        irSnippet.createThisReceiverParameter(context, IrDeclarationOrigin.INSTANCE_RECEIVER, type)
//            .transform(this, ScriptLikeToClassTransformerContext(null, null, null, false))
//    }

    override fun visitSimpleFunction(
        declaration: IrSimpleFunction,
        data: ScriptLikeToClassTransformerContext,
    ): IrSimpleFunction {
        if (declaration.parent == irSnippet || declaration.parent == irTargetClass) {
            declaration.visibility = DescriptorVisibilities.PUBLIC
        }
        return super.visitSimpleFunction(declaration, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: ScriptLikeToClassTransformerContext): IrExpression {
        val targetField = varsToFields[expression.symbol]
        return if (targetField != null) {
            createIrGetValFromState(expression.startOffset, expression.endOffset, targetField.owner.parentAsClass.symbol, targetField, data).transform(this, data)
        } else {
            super.visitGetValue(expression, data)
        }
    }

    override fun visitSetValue(
        expression: IrSetValue,
        data: ScriptLikeToClassTransformerContext,
    ): IrExpression {
        val targetField = varsToFields[expression.symbol]
        return if (targetField != null) {
            IrSetFieldImpl(
                expression.startOffset,
                expression.endOffset,
                targetField,
                expression.type,
                expression.origin,
                irTargetClass.symbol,
            ).transform(this, data)
        } else {
            super.visitSetValue(expression, data)
        }
    }

    private val mapClass = stateField.type.getClass()!!
    private val mapGet = mapClass.functions.single { it.name.asString() == "get" }
    private val mapPut = mapClass.functions.single { it.name.asString() == "put" }

    fun createIrGetValFromState(startOffset: Int, endOffset: Int, irTargetSnippetClass: IrClassSymbol, irTargetField: IrFieldSymbol, data: ScriptLikeToClassTransformerContext): IrExpression =
        IrGetFieldImpl(
            startOffset,
            endOffset,
            irTargetField,
            irTargetField.owner.type,
            IrStatementOrigin.GET_PROPERTY
        ).also {
            it.receiver = IrCallImpl(startOffset, endOffset, mapGet.returnType, mapGet.symbol).apply {
                dispatchReceiver =
                    IrGetFieldImpl(
                        startOffset, endOffset,
                        stateField.symbol,
                        irTargetSnippetClass.typeWith(),
                        IrStatementOrigin.GET_PROPERTY
                    ).also {
                        it.receiver = getAccessCallForSelf(data, startOffset, endOffset, null, null)
                    }
                putValueArgument(0, IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, irTargetSnippetClass.owner.name.asString()))
            }
        }

//    fun IrBuilderWithScope.createIrSetValInState() : IrExpression {
//
//    }
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
