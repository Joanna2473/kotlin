/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.test

import kotlinx.validation.api.*
import kotlinx.validation.api.buildGradleKts
import kotlinx.validation.api.append
import kotlinx.validation.api.test
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Assume
import org.junit.Test
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertTrue

internal const val BANNED_TARGETS_PROPERTY_NAME = "binary.compatibility.validator.klib.targets.disabled.for.testing"

private fun KlibVerificationTests.checkKlibDump(
    buildResult: BuildResult,
    expectedDumpFileName: String,
    projectName: String = "testproject",
    dumpTask: String = ":apiDump"
) {
    buildResult.assertTaskSuccess(dumpTask)

    val generatedDump = rootProjectAbiDump(projectName)
    assertTrue(generatedDump.exists(), "There are no dumps generated for KLibs")

    val expected = readFileList(expectedDumpFileName)

    Assertions.assertThat(generatedDump.readText()).isEqualTo(expected)
}

@MppGradlePluginTests
internal class KlibVerificationTests : AbiValidationBaseTests() {
    private fun BaseKotlinScope.baseProjectSetting() {
        buildGradleKts {
            append("/testProject/abi-validation/templates/gradle/base/withNativePlugin.gradle.kts")
        }
    }

    private fun BaseKotlinScope.additionalBuildConfig(config: String) {
        buildGradleKts {
            append(config)
        }
    }

    private fun BaseKotlinScope.addToSrcSet(pathTestFile: String, sourceSet: String = "commonMain") {
        val fileName = Paths.get(pathTestFile).fileName.toString()
        kotlin(fileName, sourceSet) {
            append(pathTestFile)
        }
    }

    private fun BaseKotlinScope.runApiCheck() {
        runner {
            arguments.add(":apiCheck")
        }
    }

    private fun BaseKotlinScope.runApiDump() {
        runner {
            arguments.add(":apiDump")
        }
    }

    private fun assertApiCheckPassed(buildResult: BuildResult) {
        buildResult.assertTaskSuccess(":apiCheck")
    }

//    @GradleTest
//    @DisplayName("apiDump for native targets")
//    fun dumpNativeTargets(gradleVersion: GradleVersion) {
//        val runner = prepare(gradleVersion) {
//            baseProjectSetting()
//            kotlinFile("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
//            addToSrcSet()
//            runApiDump()
//        }
//
//        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.with.linux.dump")
//    }

    @Test
    fun `apiDump for native targets in K2`() {
        val runner = test {
            settingsGradleKts {
                append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/withNativePluginK2.gradle.kts")
            }
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.with.linux.dump")
    }

    @Test
    fun `apiCheck for native targets`() {
        val runner = test {
            baseProjectSetting()

            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")

            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.dump")
            }

            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck for native targets should fail when a class is not in a dump`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/BuildConfig.kt")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/Empty.klib.dump")
            }
            runApiCheck()
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output)
                .contains("+final class com.company/BuildConfig { // com.company/BuildConfig|null[0]")
            tasks.filter { it.path.endsWith("ApiCheck") }
                .forEach {
                    assertTaskFailure(it.path)
                }
        }
    }

    @Test
    fun `apiDump should include target-specific sources`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            runApiDump()
        }

        runner.build().apply {
            checkKlibDump(
                this,
                "/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64Extra.klib.dump"
            )
        }
    }

    @Test
    fun `apiDump with native targets along with JVM target`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/base/enableJvmInWithNativePlugin.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            runApiDump()
        }

        runner.build().apply {
            checkKlibDump(this, "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")

            val jvmApiDump = rootProjectDir.resolve("$API_DIR/testproject.api")
            assertTrue(jvmApiDump.exists(), "No API dump for JVM")

            val jvmExpected = readFileList("/testProject/abi-validation/templates/classes/AnotherBuildConfig.dump")
            Assertions.assertThat(jvmApiDump.readText()).isEqualTo(jvmExpected)
        }
    }

    @Test
    fun `apiDump should ignore a class listed in ignoredClasses`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/BuildConfig.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")
    }

    @Test
    fun `apiDump should succeed if a class listed in ignoredClasses is not found`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/ignoredClasses/oneValidFullyQualifiedClass.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")
    }

    @Test
    fun `apiDump should ignore all entities from a package listed in ingoredPackages`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/ignoredPackages/oneValidPackage.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/BuildConfig.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/SubPackage.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")
    }

    @Test
    fun `apiDump should ignore all entities annotated with non-public markers`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/nonPublicMarkers/klib.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/HiddenDeclarations.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/NonPublicMarkers.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/HiddenDeclarations.klib.dump")
    }

    @Test
    fun `apiDump should not dump subclasses excluded via ignoredClasses`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/ignoreSubclasses/ignore.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/Subclasses.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/Subclasses.klib.dump")
    }

    @Test
    fun `apiCheck for native targets using v1 signatures`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/signatures/v1.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")

            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.v1.dump")
            }

            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiDump for native targets should fail when using invalid signature version`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/signatures/invalid.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            runApiDump()
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains("Unsupported KLib signature version '100500'")
        }
    }

    @Test
    fun `apiDump should work for Apple-targets`() {
        Assume.assumeTrue(HostManager().isEnabled(KonanTarget.MACOS_ARM64))
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/appleTargets/targets.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.all.dump")
    }

    @Test
    fun `apiCheck should work for Apple-targets`() {
        Assume.assumeTrue(HostManager().isEnabled(KonanTarget.MACOS_ARM64))
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/appleTargets/targets.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.all.dump")
            }
            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck should not fail if a target is not supported`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":apiCheck")
            }
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck should ignore unsupported targets by default`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                // note that the regular dump is used, where linuxArm64 is presented
                append("/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":apiCheck")
            }
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck should fail for unsupported targets with strict mode turned on`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/unsupported/enforce.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                // note that the regular dump is used, where linuxArm64 is presented
                append("/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":apiCheck")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":klibApiExtractForValidation")
        }
    }

    @Test
    fun `klibDump should infer a dump for unsupported target from similar enough target`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.with.linux.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `check sorting for target-specific declarations`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarationsExp.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarationsLinuxOnly.kt", "linuxMain")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarationsMingwOnly.kt", "mingwMain")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarationsAndroidOnly.kt", "androidNativeMain")


            runner {
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.diverging.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `infer a dump for a target with custom name`() {
        val runner = test {
            settingsGradleKts {
                append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/withNativePluginAndNoTargets.gradle.kts")
            }
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/grouping/clashingTargetNames.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt", "linuxMain")
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linux")
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.with.guessed.linux.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `klibDump should fail when the only target in the project is disabled`() {
        val runner = test {
            settingsGradleKts {
                append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/withNativePluginAndSingleTarget.gradle.kts")
            }
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            runner {
                arguments.add("-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64")
                arguments.add(":klibApiDump")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":linuxArm64ApiInfer")
            Assertions.assertThat(output).contains(
                "The target linuxArm64 is not supported by the host compiler " +
                        "and there are no targets similar to linuxArm64 to infer a dump from it."
            )
        }
    }

    @Test
    fun `klibDump if all klib-targets are unavailable`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            runner {
                arguments.add(
                    "-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64,linuxX64,mingwX64," +
                            "androidNativeArm32,androidNativeArm64,androidNativeX64,androidNativeX86"
                )
                arguments.add(":klibApiDump")
            }
        }

        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains(
                "is not supported by the host compiler and there are no targets similar to"
            )
        }
    }

    @Test
    fun `klibCheck should not fail if all klib-targets are unavailable`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                // note that the regular dump is used, where linuxArm64 is presented
                append("/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add(
                    "-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64,linuxX64,mingwX64," +
                            "androidNativeArm32,androidNativeArm64,androidNativeX64,androidNativeX86"
                )
                arguments.add(":klibApiCheck")
            }
        }

        runner.build().apply {
            assertTaskSuccess(":klibApiCheck")
        }
    }

    @Test
    fun `klibCheck should fail with strict validation if all klib-targets are unavailable`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/unsupported/enforce.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/TopLevelDeclarations.kt")
            abiFile(projectName = "testproject") {
                // note that the regular dump is used, where linuxArm64 is presented
                append("/testProject/abi-validation/templates/classes/TopLevelDeclarations.klib.dump")
            }
            runner {
                arguments.add(
                    "-P$BANNED_TARGETS_PROPERTY_NAME=linuxArm64,linuxX64,mingwX64," +
                            "androidNativeArm32,androidNativeArm64,androidNativeX64,androidNativeX86"
                )
                arguments.add(":klibApiCheck")
            }
        }

        runner.buildAndFail().apply {
            assertTaskFailure(":klibApiExtractForValidation")
        }
    }

    @Test
    fun `target name clashing with a group name`() {
        val runner = test {
            settingsGradleKts {
                append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/withNativePluginAndNoTargets.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/grouping/clashingTargetNames.gradle.kts")
            }
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            kotlin("AnotherBuildConfigLinuxX64.kt", "linuxMain") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt")
            }
            runner {
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.clash.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `target name grouping with custom target names`() {
        val runner = test {
            settingsGradleKts {
                append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
            }
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/withNativePluginAndNoTargets.gradle.kts")
                append("/testProject/abi-validation/templates/gradle/configuration/grouping/customTargetNames.gradle.kts")
            }
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            runner {
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.custom.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `target name grouping`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt", "linuxArm64Main")
            kotlin("AnotherBuildConfigLinuxX64.kt", "linuxX64Main") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt")
            }
            runner {
                arguments.add(":klibApiDump")
            }
        }

        checkKlibDump(
            runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfigLinux.klib.grouping.dump",
            dumpTask = ":klibApiDump"
        )
    }

    @Test
    fun `apiDump should work with web targets`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/nonNativeKlibTargets/targets.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            runApiDump()
        }

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.web.dump")
    }

    @Test
    fun `apiCheck should work with web targets`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/nonNativeKlibTargets/targets.gradle.kts")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.web.dump")
            }
            runApiCheck()
        }

        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `check dump is updated on added declaration`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            runApiDump()
        }
        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")

        // Update the source file by adding a declaration
        val updatedSourceFile = File(
            this::class.java.getResource(
                "/testProject/abi-validation/templates/classes/AnotherBuildConfigModified.kt"
            )!!.toURI()
        )
        val existingSource = runner.projectDir.resolve(
            "src/commonMain/kotlin/AnotherBuildConfig.kt"
        )
        Files.write(existingSource.toPath(), updatedSourceFile.readBytes())

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfigModified.klib.dump")
    }

    @Test
    fun `check dump is updated on a declaration added to some source sets`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            runApiDump()
        }
        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")

        // Update the source file by adding a declaration
        val updatedSourceFile = File(
            this::class.java.getResource(
                "/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64.kt"
            )!!.toURI()
        )
        val existingSource = runner.projectDir.resolve(
            "src/linuxArm64Main/kotlin/AnotherBuildConfigLinuxArm64.kt"
        )
        existingSource.parentFile.mkdirs()
        Files.write(existingSource.toPath(), updatedSourceFile.readBytes())

        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfigLinuxArm64Extra.klib.dump")
    }

    @Test
    fun `re-validate dump after sources updated`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")
            }
            runApiCheck()
        }
        assertApiCheckPassed(runner.build())

        // Update the source file by adding a declaration
        val updatedSourceFile = File(
            this::class.java.getResource(
                "/testProject/abi-validation/templates/classes/AnotherBuildConfigModified.kt"
            )!!.toURI()
        )
        val existingSource = runner.projectDir.resolve(
            "src/commonMain/kotlin/AnotherBuildConfig.kt"
        )
        Files.write(existingSource.toPath(), updatedSourceFile.readBytes())

        runner.buildAndFail().apply {
            assertTaskFailure(":klibApiCheck")
        }
    }

    @Test
    fun `validation should fail on target rename`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.renamedTarget.dump")
            }
            runApiCheck()
        }
        runner.buildAndFail().apply {
            Assertions.assertThat(output).contains(
                "  -// Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, " +
                        "androidNativeX86, linuxArm64.linux, linuxX64, mingwX64]"
            )
        }
    }

    @Test
    fun `apiDump should not fail for empty project`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt", sourceSet = "commonTest")
            runApiDump()
        }

        runner.build().apply {
            assertTaskSuccess(":klibApiDump")
        }
        val apiDumpFile = rootProjectAbiDump("testproject")
        assertTrue(apiDumpFile.exists())
        assertTrue(apiDumpFile.readText().isEmpty())
    }

    @Test
    fun `apiDump should dump empty file if the project does not contain sources anymore`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt", sourceSet = "commonTest")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")
            }
            runApiDump()
        }

        runner.build().apply {
            assertTaskSuccess(":klibApiDump")
        }
        val dumpFile = rootProjectAbiDump("testproject")
        assertTrue(dumpFile.exists())
        assertTrue(dumpFile.readText().isEmpty())
    }

    @Test
    fun `apiDump should not fail if there is only one target`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt", sourceSet = "commonTest")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt", sourceSet = "linuxX64Main")
            runApiDump()
        }
        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.linuxX64Only.dump")
    }

    @Test
    fun `apiCheck should fail for empty project without a dump file`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt", sourceSet = "commonTest")
            runApiCheck()
        }
        runner.buildAndFail().apply {
            assertTaskFailure(":klibApiExtractForValidation")
            Assertions.assertThat(output).contains(
                "File with project's API declarations 'api${File.separator}testproject.klib.api' does not exist."
            )
        }
    }

    @Test
    fun `apiCheck should not fail for empty project with an empty dump file`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt", sourceSet = "commonTest")
            abiFile("testproject") {
                // empty dump file
            }
            runApiCheck()
        }
        runner.build().apply {
            assertTaskSuccess(":klibApiCheck")
        }
    }

    @Test
    fun `apiDump for a project with generated sources only`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/generatedSources/generatedSources.gradle.kts")
            runner {
                arguments.add(":apiDump")
            }
        }
        checkKlibDump(runner.build(), "/testProject/abi-validation/templates/classes/GeneratedSources.klib.dump")
    }

    @Test
    fun `apiCheck for a project with generated sources only`() {
        val runner = test {
            baseProjectSetting()
            additionalBuildConfig("/testProject/abi-validation/templates/gradle/configuration/generatedSources/generatedSources.gradle.kts")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/GeneratedSources.klib.dump")
            }
            runner {
                arguments.add(":apiCheck")
            }
        }
        assertApiCheckPassed(runner.build())
    }

    @Test
    fun `apiCheck should fail after a source set was removed`() {
        val runner = test {
            baseProjectSetting()
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt", "linuxX64Main")
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt", "linuxArm64Main")
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")
            }
            runApiCheck()
        }
        runner.buildAndFail().apply {
            assertTaskFailure(":klibApiCheck")
        }
    }

    @Test
    fun `apiCheck should fail after target removal`() {
        val runner = test {
            settingsGradleKts {
                append("/testProject/abi-validation/templates/gradle/settings/settings-name-testproject.gradle.kts")
            }
            // only a single native target is defined there
            buildGradleKts {
                append("/testProject/abi-validation/templates/gradle/base/withNativePluginAndSingleTarget.gradle.kts")
            }
            addToSrcSet("/testProject/abi-validation/templates/classes/AnotherBuildConfig.kt")
            // dump was created for multiple native targets
            abiFile(projectName = "testproject") {
                append("/testProject/abi-validation/templates/classes/AnotherBuildConfig.klib.dump")
            }
            runApiCheck()
        }
        runner.buildAndFail().apply {
            assertTaskFailure(":klibApiCheck")
            Assertions.assertThat(output)
                .contains("-// Targets: [androidNativeArm32, androidNativeArm64, androidNativeX64, " +
                        "androidNativeX86, linuxArm64, linuxX64, mingwX64]")
                .contains("+// Targets: [linuxArm64]")
        }
    }
}
