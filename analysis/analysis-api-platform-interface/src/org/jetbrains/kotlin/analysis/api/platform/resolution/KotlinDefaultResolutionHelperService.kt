/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.resolution

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

@KaIdeApi
public class KotlinDefaultResolutionHelperService : KotlinResolutionHelperService {
    private val blockCounter = ThreadLocal.withInitial { BlockCounter() }

    @KaImplementationDetail
    override fun enterBlockWithCompilerContractChecks() {
        blockCounter.get().enter()
    }

    @KaImplementationDetail
    override fun exitBlockWithCompilerContractChecks() {
        blockCounter.get().exit()
    }

    override val isInsideKotlinResolver: Boolean
        get() = blockCounter.get().isInside

    private class BlockCounter {
        private var count = 0

        fun enter() {
            ++count
        }

        fun exit() {
            --count
        }

        /**
         * The service guarantees that all [enterBlockWithCompilerContractChecks] and [exitBlockWithCompilerContractChecks]
         * are paired, so 0 means there is no resolver on the stack, and more than one means ongoing resolution.
         */
        val isInside: Boolean get() = count > 0
    }
}
