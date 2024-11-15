/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@DisplayName("Build services usages in tasks are declared with `usesService`")
class BuildServiceDeclarationIT : KGPBaseTest() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        warningMode = WarningMode.Fail
    )

    @DisplayName("Build services are registered for Kotlin/JVM projects")
    @GradleTest
    @JvmGradlePluginTests
    fun testJvmProject(gradleVersion: GradleVersion) {
        project(
            "kotlinJavaProject",
            gradleVersion,
        ) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    @DisplayName("Build services are registered for Kotlin/JS browser projects")
    @GradleTest
    @JsGradlePluginTests
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun testJsBrowserProject(gradleVersion: GradleVersion) {
        project("kotlin-js-browser-project", gradleVersion) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    @DisplayName("Build services are registered for Kotlin/JS nodejs projects")
    @GradleTest
    @JsGradlePluginTests
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun testJsNodeJsProject(gradleVersion: GradleVersion) {
        project("kotlin-js-nodejs-project", gradleVersion) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    @DisplayName("Build services are registered for Kotlin/MPP projects")
    @GradleTest
    @MppGradlePluginTests
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_8_0)
    fun testMppProject(gradleVersion: GradleVersion) {
        project("new-mpp-lib-with-tests", gradleVersion) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
            }
        }
    }

    @DisplayName("Build services are registered for Kapt projects")
    @GradleTest
    @OtherGradlePluginTests
    fun testKaptProject(gradleVersion: GradleVersion) {
        project(
            "kapt2/simple",
            gradleVersion,
        ) {
            enableStableConfigurationCachePreview()
            build("build") {
                assertOutputDoesNotContainBuildServiceDeclarationWarnings()
                assertNoBuildWarnings(expectedK2KaptWarnings)
            }
        }
    }

    private fun BuildResult.assertOutputDoesNotContainBuildServiceDeclarationWarnings() {
        assertOutputDoesNotContain("without the corresponding declaration via 'Task#usesService'")
    }
}