/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlinx.jspo.compiler.backend

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.js.common.isES5IdentifierPart
import org.jetbrains.kotlin.js.common.isES5IdentifierStart
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.jspo.compiler.resolve.JsPlainObjectsPluginKey
import org.jetbrains.kotlinx.jspo.compiler.resolve.StandardIds
import kotlin.math.abs

private class MoveExternalInlineFunctionsWithBodiesOutsideLowering(private val context: IrPluginContext) : DeclarationTransformer {
    private val EXPECTED_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(JsPlainObjectsPluginKey)
    private val jsFunction = context.referenceFunctions(StandardIds.JS_FUNCTION_ID).single()

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        val file = declaration.file
        val parent = declaration.parentClassOrNull

        if (parent == null || declaration !is IrSimpleFunction || declaration.origin != EXPECTED_ORIGIN) return null

        val proxyFunction = createFunctionContainingTheLogic(declaration, parent).also(file::addChild)
        declaration.body = generateBodyWithTheProxyFunctionCall(declaration, proxyFunction)

        return null
    }

    private fun IrSimpleFunction.makeUniqueName(parent: IrClass): Name {
        val fqName = parent.fqNameWhenAvailable ?: error("Parent class must have a FQ name")
        val replacedWithUnderscoreFqn = sanitizeName(fqName.asString()).replace(".", "_")
        return Name.identifier(replacedWithUnderscoreFqn + "_" + sanitizeName(name.asString()))
    }

    private fun createFunctionContainingTheLogic(originalFunction: IrSimpleFunction, parent: IrClass): IrSimpleFunction {
        return context.irFactory.buildFun {
            updateFrom(originalFunction)
            name = originalFunction.makeUniqueName(parent)
            returnType = originalFunction.returnType
            origin = originalFunction.origin
            isInline = true
            isExternal = false
        }.apply {
            copyTypeParametersFrom(originalFunction)

            val substitutionMap = HashMap<IrTypeParameterSymbol, IrType>()
            substitutionMap.putAll(makeTypeParameterSubstitutionMap(originalFunction, this))

            if (!parent.isCompanion) {
                copyTypeParametersFrom(parent)
                substitutionMap.putAll(makeTypeParameterSubstitutionMap(parent, this))
            }

            copyValueParametersFrom(originalFunction, substitutionMap)

            val receiverParameter = dispatchReceiverParameter
            dispatchReceiverParameter = null
            extensionReceiverParameter = receiverParameter.takeIf { !parent.isCompanion }
            returnType = returnType.substitute(substitutionMap)

            body = when (originalFunction.name) {
                StandardNames.DATA_CLASS_COPY -> generateBodyForCopyFunction(this)
                OperatorNameConventions.INVOKE -> generateBodyForFactoryFunction(this)
                else -> error("Unexpected function with name `${originalFunction.name.identifier}`")
            }
        }
    }

    private fun generateBodyWithTheProxyFunctionCall(originalFunction: IrSimpleFunction, proxyFunction: IrSimpleFunction): IrBlockBody {
        return context.irFactory.createBlockBody(originalFunction.startOffset, originalFunction.endOffset).apply {
            statements += IrReturnImpl(
                originalFunction.startOffset,
                originalFunction.endOffset,
                originalFunction.returnType,
                originalFunction.symbol,
                IrCallImpl(
                    originalFunction.startOffset,
                    originalFunction.endOffset,
                    originalFunction.returnType,
                    proxyFunction.symbol,
                    proxyFunction.typeParameters.size,
                ).apply {
                    originalFunction.dispatchReceiverParameter.takeIf { originalFunction.parentClassOrNull?.isCompanion != true }?.let {
                        extensionReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.symbol)
                    }

                    for ((index, parameter) in originalFunction.valueParameters.withIndex()) {
                        putValueArgument(index, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.symbol))
                    }

                    val typeParameters = originalFunction.typeParameters.ifEmpty { originalFunction.parentAsClass.typeParameters }
                    for ((index, type) in typeParameters.withIndex()) {
                        putTypeArgument(index, type.defaultType)
                    }
                }
            )
        }
    }

    private fun generateBodyForFactoryFunction(proxyFunction: IrSimpleFunction): IrBlockBody {
        return context.irFactory.createBlockBody(proxyFunction.startOffset, proxyFunction.endOffset).apply {
            statements += IrReturnImpl(
                proxyFunction.startOffset,
                proxyFunction.endOffset,
                proxyFunction.returnType,
                proxyFunction.symbol,
                IrCallImpl(
                    proxyFunction.startOffset,
                    proxyFunction.endOffset,
                    proxyFunction.returnType,
                    jsFunction,
                    0,
                ).apply {
                    putValueArgument(0, createValueParametersObject(proxyFunction.valueParameters).toIrConst(context.irBuiltIns.stringType))
                }
            )
        }
    }

    private fun sanitizeName(name: String): String {
        if (name.isEmpty()) return "_"

        val builder = StringBuilder(name.length + 7)

        val first = name.first()

        builder.append(first.mangleIfNot(Char::isES5IdentifierStart))

        for (idx in 1..name.lastIndex) {
            val c = name[idx]
            builder.append(c.mangleIfNot(Char::isES5IdentifierPart))
        }

        return "${builder}_${abs(name.hashCode()).toString(Character.MAX_RADIX)}"
    }

    private inline fun Char.mangleIfNot(predicate: Char.() -> Boolean) =
        if (predicate()) this else '_'

    private fun generateBodyForCopyFunction(proxyFunction: IrSimpleFunction): IrBlockBody {
        return context.irFactory.createBlockBody(proxyFunction.startOffset, proxyFunction.endOffset).apply {
            val selfName = Name.identifier("${"$$"}tmp_self${"$$"}")
            statements += IrVariableImpl(
                proxyFunction.startOffset,
                proxyFunction.endOffset,
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                IrVariableSymbolImpl(),
                selfName,
                context.irBuiltIns.nothingType,
                isVar = false,
                isConst = false,
                isLateinit = false
            ).apply {
                parent = proxyFunction
                initializer = IrGetValueImpl(
                    proxyFunction.startOffset,
                    proxyFunction.endOffset,
                    proxyFunction.extensionReceiverParameter!!.symbol
                )
            }
            statements += IrReturnImpl(
                proxyFunction.startOffset,
                proxyFunction.endOffset,
                proxyFunction.returnType,
                proxyFunction.symbol,
                IrCallImpl(
                    proxyFunction.startOffset,
                    proxyFunction.endOffset,
                    proxyFunction.returnType,
                    jsFunction,
                    0,
                ).apply {
                    val objectAssignCall = "Object.assign({}, ${selfName.identifier}, ${createValueParametersObject(proxyFunction.valueParameters)})"
                    putValueArgument(0, objectAssignCall.toIrConst(context.irBuiltIns.stringType))
                }
            )
        }
    }

    private fun createValueParametersObject(valueParameters: Iterable<IrValueParameter>): String {
        val listOfParameters = valueParameters.joinToString(", ") {
            val (key, value) = it.name.run {
                if (identifier.isValidES5Identifier() && identifier !in RESERVED_KEYWORDS) identifier to identifier
                else {
                    val newName: Name = Name.identifier(sanitizeName(identifier)).apply { it.name = this }
                    "\"${identifier}\"" to newName.identifier
                }
            }
            "$key:$value"
        }

        return "{$listOfParameters}"
    }
}

open class JsPlainObjectsLoweringExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        MoveExternalInlineFunctionsWithBodiesOutsideLowering(pluginContext).lower(moduleFragment)
    }
}
