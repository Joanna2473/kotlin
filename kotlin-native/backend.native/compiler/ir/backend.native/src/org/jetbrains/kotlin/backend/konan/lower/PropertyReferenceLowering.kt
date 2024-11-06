/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

internal class PropertyReferencesConstructorsSet(
    val local: IrConstructorSymbol,
    val byRecieversCount: List<IrConstructorSymbol>
) {
    constructor(local: IrClassSymbol, byRecieversCount: List<IrClassSymbol>) : this(
            local.constructors.single(),
            byRecieversCount.map { it.constructors.single() }
    )
}

internal val KonanSymbols.immutablePropertiesConstructors
    get() = PropertyReferencesConstructorsSet(
        kLocalDelegatedPropertyImpl,
        listOf(kProperty0Impl, kProperty1Impl, kProperty2Impl)
    )

internal val KonanSymbols.mutablePropertiesConstructors
    get() = PropertyReferencesConstructorsSet(
            kLocalDelegatedMutablePropertyImpl,
            listOf(kMutableProperty0Impl, kMutableProperty1Impl, kMutableProperty2Impl)
    )

internal class PropertyReferenceLowering(val generationState: NativeGenerationState) : FileLoweringPass {
    private val context = generationState.context
    private val symbols = context.ir.symbols
    private val immutableSymbols = symbols.immutablePropertiesConstructors
    private val mutableSymbols = symbols.mutablePropertiesConstructors

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitRichPropertyReference(expression: IrRichPropertyReference): IrExpression {
                expression.transformChildrenVoid(this)
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression).toNativeConstantReflectionBuilder(context.ir.symbols)
                val originalPropertySymbol = expression.reflectionTargetSymbol
                if (originalPropertySymbol is IrLocalDelegatedPropertySymbol) {
                    return irBuilder.createLocalKProperty(originalPropertySymbol.owner.name.asString(), expression.type)
                }
                require(originalPropertySymbol is IrPropertySymbol)
                val typeArguments = (expression.type as IrSimpleType).arguments.map { it.typeOrNull ?: context.irBuiltIns.anyNType  }
                val block =  irBuilder.irBlock {
                    val captures = expression.boundValues.map {
                        if (it is IrGetValue) it.symbol.owner else irTemporary(it)
                    }

                    val constructor = if (expression.setterFunction != null) {
                        mutableSymbols
                    } else {
                        immutableSymbols
                    }.byRecieversCount[typeArguments.size - 1]

                    +irCall(constructor, expression.type, typeArguments).apply {
                        arguments[0] = irString(originalPropertySymbol.owner.name.asString())
                        val getterType = symbols.kFunctionN(typeArguments.size - 1).typeWith(typeArguments)
                        arguments[1] = IrRichFunctionReferenceImpl(
                                startOffset = expression.startOffset,
                                endOffset = expression.endOffset,
                                type = getterType,
                                reflectionTargetSymbol = originalPropertySymbol.owner.getter!!.symbol,
                                overriddenFunctionSymbol = UpgradeCallableReferences.selectSAMOverriddenFunction(getterType),
                                invokeFunction = expression.getterFunction,
                                origin = expression.origin
                        ).apply {
                            boundValues += captures.map { irGet(it) }
                        }
                        expression.setterFunction?.let { setterFunction ->
                            val setterType = symbols.kFunctionN(typeArguments.size).typeWith(typeArguments + context.irBuiltIns.unitType)
                            val reference = IrRichFunctionReferenceImpl(
                                    startOffset = expression.startOffset,
                                    endOffset = expression.endOffset,
                                    type = setterType,
                                    reflectionTargetSymbol = originalPropertySymbol.owner.setter!!.symbol,
                                    invokeFunction = setterFunction,
                                    overriddenFunctionSymbol = UpgradeCallableReferences.selectSAMOverriddenFunction(setterType),
                                    origin = expression.origin
                            )
                            reference.boundValues += captures.map { irGet(it) }
                            arguments[2] = reference
                        }
                    }
                }
                if (expression.boundValues.isEmpty()) {
                    return block.statements.single() as IrExpression
                }
                return block
            }

            override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
                shouldNotBeCalled("Property references should've been lowered at this point")
            }

            override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
                shouldNotBeCalled("Property references should've been lowered at this point")
            }
        })
    }

    private fun NativeConstantReflectionIrBuilder.createLocalKProperty(propertyName: String,
                                                                       propertyType: IrType): IrConstantValue {
        val symbols = this@PropertyReferenceLowering.context.ir.symbols
        return irConstantObject(
                symbols.kLocalDelegatedPropertyImpl.owner,
                mapOf(
                        "name" to irConstantPrimitive(irString(propertyName)),
                        "returnType" to irKType(propertyType)
                )
        )
    }
}
