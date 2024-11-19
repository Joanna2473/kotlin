/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.junit.*
import kotlin.test.assertTrue

class NonPublicMarkersTest : AbiValidationBaseTests() {

    @Test
    fun testIgnoredMarkersOnProperties() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
            }

            kotlin("Properties.kt") {
                append("/testProject/abi-validation/templates/classes/Properties.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                append("/testProject/abi-validation/templates/classes/Properties.dump")
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    @Ignore("https://youtrack.jetbrains.com/issue/KT-62259")
    fun testIgnoredMarkersOnPropertiesForNativeTargets() {
        val runner = test {
            settingsGradleKts {
                append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
            }

            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/withNativePlugin.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
            }

            kotlin("Properties.kt", sourceSet = "commonMain") {
                append("/testProject/abi-validation/templates/classes/Properties.kt")
            }

            commonNativeTargets.forEach {
                abiFile(projectName = "testproject", target = it) {
                    append("/testProject/abi-validation/templates/classes/Properties.klib.dump")
                }
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    fun testFiltrationByPackageLevelAnnotations() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/nonPublicMarkers/packages.gradle.kts")
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

            val expected = readFileList("/testProject/abi-validation/templates/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

    @Test
    fun testIgnoredMarkersOnConstProperties() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/nonPublicMarkers/markers.gradle.kts")
            }

            kotlin("ConstProperty.kt") {
                append("/testProject/abi-validation/templates/classes/ConstProperty.kt")
            }

            apiFile(projectName = rootProjectDir.name) {
                append("/testProject/abi-validation/templates/classes/ConstProperty.dump")
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.withDebug(true).build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }
}
