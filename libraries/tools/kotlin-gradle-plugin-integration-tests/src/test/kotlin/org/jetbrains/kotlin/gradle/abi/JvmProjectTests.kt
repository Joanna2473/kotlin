/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.append
import kotlinx.validation.api.test
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.junit.Test
import org.junit.jupiter.api.DisplayName

@JvmGradlePluginTests
class JvmProjectTests : AbiValidationBaseTests() {
    @GradleTest
    @DisplayName("a1")
    fun `apiDump for a project with generated sources only`(gradleVersion: GradleVersion) {
        val runner = prepare(gradleVersion) {
            buildFile.appendFile("/testProject/abi-validation/templates/gradle/configuration/generatedSources/generatedJvmSources.gradle.kts")
            jvmAbiFile.appendFile("/testProject/abi-validation/templates/classes/GeneratedSources.dump")
        }
        runner.build(":dumpAbi") {
            assertTaskSuccess(":dumpAbi")

            assertEqualIgnoringNewLines(
                resource("/testProject/abi-validation/templates/classes/GeneratedSources.dump").readText(),
                actualJvmAbiFile.readText()
            )
        }
        runner.build(":checkAbi") {
            assertTaskSuccess(":checkAbi")
        }
    }
}
