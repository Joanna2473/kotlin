/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.junit.Test

class MixedMarkersTest : AbiValidationBaseTests() {

    @Test
    fun testMixedMarkers() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/publicMarkers/mixedMarkers.gradle.kts")
            }

            kotlin("MixedAnnotations.kt") {
                append("/testProject/abi-validation/templates/classes/MixedAnnotations.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                append("/testProject/abi-validation/templates/classes/MixedAnnotations.dump")
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }
}
