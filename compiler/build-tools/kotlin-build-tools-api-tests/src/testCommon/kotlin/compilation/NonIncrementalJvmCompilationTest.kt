/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunnerProvider
import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.prepareModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.exists

@DisplayName("Smoke tests for non-incremental compilation via the build tools API")
internal class NonIncrementalJvmCompilationTest : BaseCompilationTest() {
    @CompilationTest
    fun smokeSingleModuleTest(buildRunnerProvider: BuildRunnerProvider) {
        scenario(buildRunnerProvider) {
            val module1 = module("jvm-module1")

            compileAll {
                expectSuccess(module1) {
                    outputFiles("FooKt.class", "Bar.class", "BazKt.class")
                }
            }
        }
    }

    @CompilationTest
    fun smokeMultipleModulesTest(buildRunnerProvider: BuildRunnerProvider) {
        scenario(buildRunnerProvider) {
            val module1 = module("jvm-module1")
            val module2 = module("jvm-module2") {
                dependsOn(module1)
            }

            compileAll {
                expectSuccess(module1) {
                    outputFiles("FooKt.class", "Bar.class", "BazKt.class")
                }
                expectSuccess(module2) {
                    outputFiles("AKt.class", "BKt.class")
                }
            }
        }
    }
}