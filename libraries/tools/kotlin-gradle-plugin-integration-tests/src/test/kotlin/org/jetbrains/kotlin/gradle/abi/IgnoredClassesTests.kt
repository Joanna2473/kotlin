/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.AbiValidationBaseTests
import kotlinx.validation.api.assertTaskSuccess
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.kotlin
import kotlinx.validation.api.readFileList
import kotlinx.validation.api.append
import kotlinx.validation.api.runner
import kotlinx.validation.api.test
import org.junit.Test
import kotlin.test.assertTrue

internal class IgnoredClassesTests : AbiValidationBaseTests() {

    @Test
    fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/BuildConfig.kt")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    fun `apiCheck should succeed, when given class is not in api-File, but is ignored via ignoredPackages`() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/ignoredPackages/oneValidPackage.gradle.kts")
            }

            kotlin("BuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/BuildConfig.kt")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    fun `apiDump should not dump ignoredClasses, when class is excluded via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }
            kotlin("BuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/BuildConfig.kt")
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
    fun `apiDump should dump class whose name is a subsset of another class that is excluded via ignoredClasses`() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            }
            kotlin("BuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/BuildConfig.kt")
            }
            kotlin("BuildCon.kt") {
                append("/testProject/abi-validation/templates/classes/BuildCon.kt")
            }

            runner {
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            assertTrue(rootProjectApiDump.exists(), "api dump file should exist")

            val expected = readFileList("/testProject/abi-validation/templates/classes/BuildCon.dump")
            Assertions.assertThat(rootProjectApiDump.readText()).isEqualToIgnoringNewLines(expected)
        }
    }
}
