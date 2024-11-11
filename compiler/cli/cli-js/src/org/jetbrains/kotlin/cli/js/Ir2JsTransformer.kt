/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.backend.js.LoweredIr
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.WholeWorldStageController
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputsBuilt
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsCodeGenerator
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic

class Ir2JsTransformer(
    val arguments: K2JSCompilerArguments,
    val module: ModulesStructure,
    val phaseConfig: PhaseConfig,
    val messageCollector: MessageCollector,
    val mainCallArguments: List<String>?,
) {
    private val performanceManager = module.compilerConfiguration[CLIConfigurationKeys.PERF_MANAGER]

    private fun lowerIr(): LoweredIr {
        return compile(
            mainCallArguments,
            module,
            phaseConfig,
            IrFactoryImplForJsIC(WholeWorldStageController()),
            keep = arguments.irKeep?.split(",")
                ?.filterNot { it.isEmpty() }
                ?.toSet()
                ?: emptySet(),
            dceRuntimeDiagnostic = RuntimeDiagnostic.resolve(
                arguments.irDceRuntimeDiagnostic,
                messageCollector
            ),
            safeExternalBoolean = arguments.irSafeExternalBoolean,
            safeExternalBooleanDiagnostic = RuntimeDiagnostic.resolve(
                arguments.irSafeExternalBooleanDiagnostic,
                messageCollector
            ),
            granularity = arguments.granularity,
        )
    }

    private fun makeJsCodeGenerator(): JsCodeGenerator {
        val ir = lowerIr()
        val transformer = IrModuleToJsTransformer(ir.context, ir.moduleFragmentToUniqueName, mainCallArguments != null)

        val mode = TranslationMode.fromFlags(arguments.irDce, arguments.granularity, arguments.irMinimizedMemberNames)
        return transformer
            .also { performanceManager?.notifyIRGenerationStarted() }
            .makeJsCodeGenerator(ir.allModules, mode)
    }

    fun compileAndTransformIrNew(): CompilationOutputsBuilt {
        return makeJsCodeGenerator()
            .generateJsCode(relativeRequirePath = true, outJsProgram = false)
            .also {
                performanceManager?.notifyIRGenerationFinished()
                performanceManager?.notifyGenerationFinished()
            }
    }
}
