/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.junit.Test
import java.io.File

internal class MultiPlatformSingleJvmTargetTest : AbiValidationBaseTests() {
    private fun BaseKotlinScope.createProjectHierarchyWithPluginOnRoot() {
        settingsGradleKts {
            append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
        }
        buildGradleKts {
            append("/testProject/abi-validation/templates/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
        }
    }

    @Test
    fun testApiCheckPasses() {
        val runner = test {
                createProjectHierarchyWithPluginOnRoot()
                runner {
                    arguments.add(":apiCheck")
                    arguments.add("--stacktrace")
                }

                dir("$API_DIR/") {
                    file("testproject.api") {
                        append("/testProject/abi-validation/templates/classes/Subsub1Class.dump")
                        append("/testProject/abi-validation/templates/classes/Subsub2Class.dump")
                    }
                }

                dir("src/jvmMain/kotlin") {}
                kotlin("Subsub1Class.kt", "commonMain") {
                    append("/testProject/abi-validation/templates/classes/Subsub1Class.kt")
                }
                kotlin("Subsub2Class.kt", "jvmMain") {
                    append("/testProject/abi-validation/templates/classes/Subsub2Class.kt")
                }

            }

        runner.build().apply {
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    fun testApiCheckFails() {
        val runner = test {
                createProjectHierarchyWithPluginOnRoot()
                runner {
                    arguments.add("--continue")
                    arguments.add(":check")
                    arguments.add("--stacktrace")
                }

                dir("$API_DIR/") {
                    file("testproject.api") {
                        append("/testProject/abi-validation/templates/classes/Subsub2Class.dump")
                        append("/testProject/abi-validation/templates/classes/Subsub1Class.dump")
                    }
                }

                dir("src/jvmMain/kotlin") {}
                kotlin("Subsub1Class.kt", "commonMain") {
                    append("/testProject/abi-validation/templates/classes/Subsub1Class.kt")
                }
                kotlin("Subsub2Class.kt", "jvmMain") {
                    append("/testProject/abi-validation/templates/classes/Subsub2Class.kt")
                }

            }

        runner.buildAndFail().apply {
            assertTaskFailure(":jvmApiCheck")
            assertTaskNotRun(":apiCheck")
            Assertions.assertThat(output).contains("API check failed for project testproject")
            assertTaskNotRun(":check")
        }
    }

    @Test
    fun testApiDumpPasses() {
        val runner = test {
                createProjectHierarchyWithPluginOnRoot()

                runner {
                    arguments.add(":apiDump")
                    arguments.add("--stacktrace")
                }

                dir("src/jvmMain/kotlin") {}
                kotlin("Subsub1Class.kt", "commonMain") {
                    append("/testProject/abi-validation/templates/classes/Subsub1Class.kt")
                }
                kotlin("Subsub2Class.kt", "jvmMain") {
                    append("/testProject/abi-validation/templates/classes/Subsub2Class.kt")
                }

            }

        runner.build().apply {
            assertTaskSuccess(":apiDump")

            val commonExpectedApi = readFileList("/testProject/abi-validation/templates/classes/Subsub1Class.dump")

            val mainExpectedApi = commonExpectedApi + "\n" + readFileList("/testProject/abi-validation/templates/classes/Subsub2Class.dump")
            Assertions.assertThat(jvmApiDump.readText()).isEqualToIgnoringNewLines(mainExpectedApi)
        }
    }

    @Test
    fun testApiDumpPassesForEmptyProject() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
            }

            runner {
                arguments.add(":apiDump")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":jvmApiDump")
            assertTaskSuccess(":apiDump")
        }
    }

    @Test
    fun testApiCheckPassesForEmptyProject() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
            }

            emptyApiFile(projectName = rootProjectDir.name)

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":jvmApiCheck")
            assertTaskSuccess(":apiCheck")
        }
    }

    @Test
    fun testApiCheckFailsForEmptyProjectWithoutDumpFile() {
        val runner = test {
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/multiplatformWithSingleJvmTarget.gradle.kts")
            }

            runner {
                arguments.add(":apiCheck")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":jvmApiCheck")
            Assertions.assertThat(output).contains(
                "Expected file with API declarations 'api${File.separator}${rootProjectDir.name}.api' does not exist"
            )
        }
    }

    private val jvmApiDump: File get() = rootProjectDir.resolve("$API_DIR/testproject.api")

}
