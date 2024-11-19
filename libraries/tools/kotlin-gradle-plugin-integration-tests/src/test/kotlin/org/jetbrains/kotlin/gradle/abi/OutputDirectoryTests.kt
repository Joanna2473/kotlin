/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.append
import kotlinx.validation.api.test
import org.junit.Test
import kotlin.test.assertTrue

class OutputDirectoryTests : AbiValidationBaseTests() {
    @Test
    fun dumpIntoCustomDirectory() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/outputDirectory/different.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            }
            dir("api") {
                file("letMeBe.txt") {
                }
            }

            runner {
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            val dumpFile = rootProjectDir.resolve("custom").resolve("${rootProjectDir.name}.api")
            assertTrue(dumpFile.exists(), "api dump file ${dumpFile.path} should exist")

            val expected = readFileList("/testProject/abi-validation/templates/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(dumpFile.readText()).isEqualToIgnoringNewLines(expected)

            val fileInsideDir = rootProjectDir.resolve("api").resolve("letMeBe.txt")
            assertTrue(fileInsideDir.exists(), "existing api directory should not be overridden")
        }
    }

    @Test
    fun validateDumpFromACustomDirectory() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/outputDirectory/different.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            }
            dir("custom") {
                file("${rootProjectDir.name}.api") {
                    append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.dump")
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
    fun dumpIntoSubdirectory() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/outputDirectory/subdirectory.gradle.kts")
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

            val dumpFile = rootProjectDir.resolve("validation")
                .resolve("api")
                .resolve("${rootProjectDir.name}.api")

            assertTrue(dumpFile.exists(), "api dump file ${dumpFile.path} should exist")

            val expected = readFileList("/testProject/abi-validation/templates/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(dumpFile.readText()).isEqualToIgnoringNewLines(expected)
        }
    }

    @Test
    fun validateDumpFromASubdirectory() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/outputDirectory/subdirectory.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            }
            dir("validation") {
                dir("api") {
                    file("${rootProjectDir.name}.api") {
                        append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.dump")
                    }
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
    fun dumpIntoParentDirectory() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/kotlinJvm.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/outputDirectory/outer.gradle.kts")
            }

            kotlin("AnotherBuildConfig.kt") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            }

            runner {
                arguments.add(":apiDump")
            }
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains("apiDumpDirectory (\"../api\") should be inside the project directory")
        }
    }
}
