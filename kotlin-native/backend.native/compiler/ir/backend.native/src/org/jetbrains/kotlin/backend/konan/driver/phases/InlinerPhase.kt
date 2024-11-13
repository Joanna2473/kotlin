/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PreSerializationPhaseContext
import org.jetbrains.kotlin.backend.konan.performanceManager
import org.jetbrains.kotlin.backend.konan.trackIRLowering
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal fun <T : PhaseContext> PhaseEngine<T>.runIrInliner(fir2IrOutput: Fir2IrOutput) {
    // TODO KT-72915 create and pass bacend context
    useContext(PreSerializationPhaseContext(this.context.config, fir2IrOutput.fir2irActualizedResult.irBuiltIns)) { inlinerEngine ->
        val irModule = fir2IrOutput.fir2irActualizedResult.irModuleFragment
        listOf(irModule to inlinerEngine).runLoweringsOfTheFirstStage()
    }
}

private fun <T : PreSerializationPhaseContext> PhaseEngine<T>.runEngineForFirstPhaseLowerings(block: PhaseEngine<T>.() -> Unit) {
    try {
        context.configuration.performanceManager.trackIRLowering {
            this.block()
        }
    } catch (t: Throwable) {
        this.context.dispose()
        throw t
    }
}

@Suppress("unused")
private fun <T : PreSerializationPhaseContext> PhaseEngine<T>.runSpecifiedLowerings(irModule: IrModuleFragment, loweringsToLaunch: FirstPhaseLoweringList) {
    runEngineForFirstPhaseLowerings {
        partiallyLowerModuleWithDependencies(irModule, loweringsToLaunch)
    }
}

@Suppress("unused")
private fun <T : PreSerializationPhaseContext> PhaseEngine<T>.runSpecifiedLowerings(irModule: IrModuleFragment, moduleLowering: FirstPhaseModuleLowering) {
    runEngineForFirstPhaseLowerings {
        partiallyLowerModuleWithDependencies(irModule, moduleLowering)
    }
}

@Suppress("UnusedReceiverParameter")
internal fun <T : PreSerializationPhaseContext> List<Pair<IrModuleFragment, PhaseEngine<T>>>.runLoweringsOfTheFirstStage() {
    // TODO KT-72439 move lowering here
}

internal fun <T : PreSerializationPhaseContext> PhaseEngine<T>.partiallyLowerModuleWithDependencies(module: IrModuleFragment, loweringList: FirstPhaseLoweringList) {
    runLowerings(loweringList, module)
}

internal fun <T : PreSerializationPhaseContext> PhaseEngine<T>.partiallyLowerModuleWithDependencies(module: IrModuleFragment, lowering: FirstPhaseModuleLowering) {
    runModuleWisePhase(lowering, module)
}
