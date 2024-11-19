/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JdkVersions
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.junit.jupiter.api.DisplayName
import kotlin.test.*

@JvmGradlePluginTests
internal class DefaultConfigTests : AbiValidationBaseTests() {

    @GradleTest
    @DisplayName("apiCheck should fail, when there is no api directory, even if there are no Kotlin sources")
    @JdkVersions(versions = [JavaVersion.VERSION_11])
    fun noReferenceDump(gradleVersion: GradleVersion) {
        val runner = prepare(gradleVersion)

        runner.buildAndFail(":checkAbi") {
            assertLogContains(
                "Expected file with ABI declarations 'abi/jvm.abi' does not exist."
            )
            assertLogContains(
                "Please ensure that 'updateAbi' was executed in order to get an ABI dump to compare the build against"
            )
            assertTaskFailure(":checkAbi")
        }
    }

    @GradleTest
    @DisplayName("apiCheck should succeed, when api-File is empty, but no kotlin files are included in SourceSet")
    fun emptyReferenceDump(gradleVersion: GradleVersion) {
        val runner = prepare(gradleVersion) {
            jvmAbiFile()
        }

        runner.build(":checkAbi") {
            assertTaskSuccess(":checkAbi")
        }
    }

    @GradleTest
    @DisplayName("apiCheck should succeed when public classes match api file")
    fun successfulCheck(gradleVersion: GradleVersion) {
        val runner = prepare(gradleVersion) {
            kotlinFile("AnotherBuildConfig.kt") {
                appendFile("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            }
            jvmAbiFile.appendFile("/testProject/abi-validation/templates/classes/AnotherBuildConfig.dump")
        }

        runner.build(":checkAbi") {
            assertTaskSuccess(":checkAbi")
        }
    }

    @GradleTest
    @DisplayName("apiCheck should fail, when a public class is not in api-File")
    fun differentDumps(gradleVersion: GradleVersion) {
        val runner = prepare(gradleVersion) {
            kotlinFile("BuildConfig.kt") {
                appendFile("/testProject/abi-validation/templates/classes/BuildConfig.kt")
            }
            jvmAbiFile()
        }

        runner.buildAndFail(":checkAbi") {
            val dumpOutput =
                "  @@ -1,1 +1,7 @@\n" +
                        "  +public final class com/company/BuildConfig {\n" +
                        "  +\tpublic fun <init> ()V\n" +
                        "  +\tpublic final fun function ()I\n" +
                        "  +\tpublic final fun getProperty ()I\n" +
                        "  +}"

            assertTaskFailure(":checkAbi")
            assertLogContains(dumpOutput)
        }
    }

    @GradleTest
    @DisplayName("apiDump should create empty api file when there are no Kotlin sources")
    fun generateEmptyDump(gradleVersion: GradleVersion) {
        val runner = prepare(gradleVersion)

        runner.build(":dumpAbi") {
            assertTaskSuccess(":dumpAbi")
            assertTrue(actualJvmAbiFile.exists(), "JVM ABI dump file should exist")
            assertEquals("", actualJvmAbiFile.readText())
        }
    }

    @GradleTest
    @DisplayName("apiDump should dump public classes")
    fun expectedActualDump(gradleVersion: GradleVersion) {
        val runner = prepare(gradleVersion) {
            kotlinFile("AnotherBuildConfig.kt") {
                appendFile("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            }
        }

        runner.build("dumpAbi") {
            assertTaskSuccess(":dumpAbi")
            assertTrue(actualJvmAbiFile.exists(), "JVM ABI dump file should exist")

            assertEqualIgnoringNewLines(
                resource("/testProject/abi-validation/templates/classes/AnotherBuildConfig.dump").readText(),
                actualJvmAbiFile.readText()
            )
        }
    }

    @GradleTest
    @DisplayName("apiCheck should not be run when we run check")
    fun checkTaskIsNotCalled(gradleVersion: GradleVersion) {
        val runner = prepare(gradleVersion)
        runner.build(":check") {
            assertTaskUpToDate(":check")
            assertTaskNotExecuted(":checkAbi")
        }
    }
}
