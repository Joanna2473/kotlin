/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolver
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.inline.PreSerializationLoweringPhasesProvider

private object NativePreSerializationLoweringPhasesProvider : PreSerializationLoweringPhasesProvider<PreSerializationLoweringContext>() {

    override val klibAssertionWrapperLowering: ((PreSerializationLoweringContext) -> FileLoweringPass)?
        get() = null // TODO(KT-71415): Return the actual lowering here

    override fun inlineFunctionResolver(context: PreSerializationLoweringContext, inlineMode: InlineMode): InlineFunctionResolver =
            TODO("Refactor NativeInlineFunctionResolver to support PreSerializationLoweringContext")
}

internal fun <T : PhaseContext> PhaseEngine<T>.runIrInliner(fir2IrOutput: Fir2IrOutput, environment: KotlinCoreEnvironment): Fir2IrOutput =
        fir2IrOutput.copy(
                fir2irActualizedResult = runPreSerializationLoweringPhases(
                        fir2IrOutput.fir2irActualizedResult,
                        NativePreSerializationLoweringPhasesProvider,
                        environment.configuration
                )
        )
