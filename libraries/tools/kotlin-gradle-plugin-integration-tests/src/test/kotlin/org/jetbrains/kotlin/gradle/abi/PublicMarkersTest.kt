/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.kotlin
import kotlinx.validation.api.append
import kotlinx.validation.api.test
import org.junit.Test
import kotlin.test.assertTrue

class PublicMarkersTest : AbiValidationBaseTests() {

    @Test
    fun testPublicMarkers() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/publicMarkers/markers.gradle.kts")
            }

            kotlin("ClassWithPublicMarkers.kt") {
                append("/testProject/abi-validation/templates/classes/ClassWithPublicMarkers.kt")
            }

            kotlin("ClassInPublicPackage.kt") {
                append("/testProject/abi-validation/templates/classes/ClassInPublicPackage.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                append("/testProject/abi-validation/templates/classes/ClassWithPublicMarkers.dump")
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    // Public markers are not supported in KLIB ABI dumps
    @Test
    fun testPublicMarkersForNativeTargets() {
        val runner = test {
            settingsGradleKts {
                append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
            }

            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/withNativePlugin.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/publicMarkers/markers.gradle.kts")
            }

            kotlin("ClassWithPublicMarkers.kt", sourceSet = "commonMain") {
                append("/testProject/abi-validation/templates/classes/ClassWithPublicMarkers.kt")
            }

            kotlin("ClassInPublicPackage.kt", sourceSet = "commonMain") {
                append("/testProject/abi-validation/templates/classes/ClassInPublicPackage.kt")
            }

            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/ClassWithPublicMarkers.klib.dump")
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.withDebug(true).build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    fun testFiltrationByPackageLevelAnnotations() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/publicMarkers/packages.gradle.kts")
            }
            java("annotated/PackageAnnotation.java") {
                append("/testProject/abi-validation/templates/classes/PackageAnnotation.java")
            }
            java("annotated/package-info.java") {
                append("/testProject/abi-validation/templates/classes/package-info.java")
            }
            kotlin("ClassFromAnnotatedPackage.kt") {
                append("/testProject/abi-validation/templates/classes/ClassFromAnnotatedPackage.kt")
            }
            kotlin("AnotherBuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            }
            runner {
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/testProject/abi-validation/templates/classes/AnnotatedPackage.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }
}
