/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.PRESETS_DEPRECATION_MESSAGE_SUFFIX
import org.jetbrains.kotlin.gradle.dsl.KotlinSourceSetConvention.isAccessedByKotlinSourceSetConventionAt
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_IMPL
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.properties.NativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV1
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.jetbrains.kotlin.gradle.utils.prettyName
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy
import java.io.File


@InternalKotlinGradlePluginApi // used in integration tests
object KotlinToolingDiagnostics {
    /**
     * This diagnostic is suppressed in kotlin-test and kotlin-stdlib.
     * We should migrate the stdlib and kotlin-test from deprecated flags and then completely remove the support.
     * ETA: 2.0-M1
     *
     * P.s. Some tests also suppress this diagnostic -- these tests should be removed together with the flags support
     */
    object PreHMPPFlagsError : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(usedDeprecatedFlags: List<String>) = build {
            name {
                "Deprecated Kotlin Multiplatform Properties"
            }
            message {
                """
                The following properties are obsolete and no longer supported:
                ${usedDeprecatedFlags.joinToString()}
                """.trimIndent()
            }
            documentation("https://kotlinlang.org/docs/multiplatform-compatibility-guide.html#deprecate-hmpp-properties") { url ->
                "Read the details here: $url"
            }
        }
    }

    object DeprecatedKotlinNativeTargetsDiagnostic : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(usedTargetIds: List<String>) = build(
            "Deprecated Kotlin/Native Targets",
            "The following removed Kotlin/Native targets were used in the project: ${usedTargetIds.joinToString()}"
        )
    }

    object CommonMainOrTestWithDependsOnDiagnostic : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(suffix: String) = build(
            "Invalid `dependsOn` Configuration in Common Source Set",
            "common$suffix can't declare dependsOn on other source sets"
        )
    }

    object NativeStdlibIsMissingDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(changedKotlinNativeHomeProperty: String?) = build {
            name {
                "Missing Kotlin/Native Standard Library"
            }
            message {
                "The Kotlin/Native distribution used in this build does not provide the standard library."
            }
            solution {
                "Make sure that the '$changedKotlinNativeHomeProperty' property points to a valid Kotlin/Native distribution."
                    .onlyIf(changedKotlinNativeHomeProperty != null)
            }
        }
    }

    object NewNativeVersionDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(nativeVersion: KotlinToolingVersion?, kotlinVersion: KotlinToolingVersion) = build {
            name {
                "Kotlin/Native and Kotlin Versions Incompatible"
            }
            message {
                "'$nativeVersion' Kotlin/Native is being used with an older '$kotlinVersion' Kotlin."
            }
            solution {
                "Please adjust versions to avoid incompatibilities."
            }
        }
    }

    object OldNativeVersionDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(nativeVersion: KotlinToolingVersion?, kotlinVersion: KotlinToolingVersion) = build {
            name {
                "Kotlin/Native and Kotlin Versions Incompatible"
            }
            message {
                "'$nativeVersion' Kotlin/Native is being used with an newer '$kotlinVersion' Kotlin."
            }
            solution {
                "Please adjust versions to avoid incompatibilities."
            }
        }
    }

    object DeprecatedJvmWithJavaPresetDiagnostic : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke() = build {
            name {
                "Deprecated 'jvmWithJava' Preset"
            }
            message {
                """
                The 'jvmWithJava' preset is deprecated and will be removed soon.
                Please use an ordinary JVM target with Java support:
                ```
                kotlin {
                    jvm {
                        withJava()
                    }
                }
                ```
                After this change, please move the Java sources to the Kotlin source set directories.
                For example, if the JVM target is given the default name 'jvm':
                 * instead of 'src/main/java', use 'src/jvmMain/java'
                 * instead of 'src/test/java', use 'src/jvmTest/java'
                """.trimIndent()
            }
        }
    }

    object UnusedSourceSetsWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(sourceSetNames: Collection<String>) = build {
            name { "Unused Kotlin Source Sets" }
            message {
                if (sourceSetNames.size == 1) {
                    "The Kotlin source set ${sourceSetNames.single()} was configured but not added to any Kotlin compilation.\n"
                } else {
                    val sourceSetNamesString = sourceSetNames.joinToString("\n") { " * $it" }
                    "The following Kotlin source sets were configured but not added to any Kotlin compilation:\n" +
                            sourceSetNamesString
                }
            }
            solution {
                "You can add a source set to a target's compilation by connecting it with the compilation's default source set using 'dependsOn'."
            }
            documentation("https://kotl.in/connecting-source-sets") { url ->
                "See $url"
            }
        }
    }

    object MultipleSourceSetRootsInCompilation : ToolingDiagnosticFactory(WARNING) {
        private fun diagnosticName() = "Missing 'dependsOn' in Source Sets"

        operator fun invoke(
            kotlinCompilation: KotlinCompilation<*>,
            unexpectedSourceSetRoot: String,
            expectedRoot: String,
        ): ToolingDiagnostic = build {
            name(::diagnosticName)
            message {
                """
                Kotlin Source Set '$unexpectedSourceSetRoot' is included to '${kotlinCompilation.name}' compilation of '${kotlinCompilation.target.name}' target,
                but it doesn't depend on '$expectedRoot'.
                
                Please remove '$unexpectedSourceSetRoot' and include its sources to the compilation's default source set:
                
                    kotlin.sourceSets["${kotlinCompilation.defaultSourceSet.name}"].kotlin.srcDir() // <-- pass sources directory of '$unexpectedSourceSetRoot'
                
                Or provide explicit dependency if the solution above is not applicable
                
                    kotlin.sourceSets["$unexpectedSourceSetRoot"].dependsOn($expectedRoot)
                """.trimIndent()
            }
            documentation("https://kotl.in/connecting-source-sets")
        }

        operator fun invoke(
            targetNames: Collection<String>,
            unexpectedSourceSetRoot: String,
            expectedRoot: String,
        ): ToolingDiagnostic = build {
            name(::diagnosticName)
            message {
                """
                Kotlin Source Set '$unexpectedSourceSetRoot' is included in compilations of Kotlin Targets: ${targetNames.joinToString(", ") { "'$it'" }}
                but it doesn't depend on '$expectedRoot'
                
                Please remove '$unexpectedSourceSetRoot' and include its sources to one of the default source set: https://kotl.in/hierarchy-template
                For example:

                    kotlin.sourceSets.commonMain.kotlin.srcDir() // <-- pass here sources directory

                Or add explicit dependency if the solution above is not applicable:

                    kotlin.sourceSets["$unexpectedSourceSetRoot"].dependsOn($expectedRoot)
                """.trimIndent()
            }
            documentation("https://kotl.in/connecting-source-sets")
        }

        operator fun invoke(kotlinCompilation: KotlinCompilation<*>, sourceSetRoots: Collection<String>) = build {
            name(::diagnosticName)
            message {
                """
                Kotlin Source Sets: ${sourceSetRoots.joinToString(", ") { "'$it'" }}
                are included to '${kotlinCompilation.name}' compilation of '${kotlinCompilation.target.name}' target.
                However, they have no common source set root between them.
                
                Please remove these kotlin source sets and include their source directories to the compilation's default source set.
                
                    kotlin.sourceSets["${kotlinCompilation.defaultSourceSet.name}"].kotlin.srcDir() // <-- pass sources directories here
                
                Or, if the solution above is not applicable, specify `dependsOn` edges between these source sets so that there are no multiple roots.
                """.trimIndent()
            }
            documentation("https://kotl.in/connecting-source-sets") { url ->
                "See $url for more details."
            }
        }
    }

    object AndroidSourceSetLayoutV1Deprecation : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke() = build {
            name {
                "Deprecated Android Source Set Layout V1"
            }
            message {
                "The version 1 of Android source set layout is deprecated. Please remove kotlin.mpp.androidSourceSetLayoutVersion=1 from the gradle.properties file."
            }
            documentation("https://kotl.in/android-source-set-layout-v2") { url ->
                "Learn how to migrate to the version 2 source set layout at: $url"
            }
        }
    }

    object AgpRequirementNotMetForAndroidSourceSetLayoutV2 : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(minimumRequiredAgpVersion: String, currentAgpVersion: String) = build(
            "Android Gradle Plugin Version Incompatible with Source Set Layout V2",
            """
            ${multiplatformAndroidSourceSetLayoutV2.name} requires Android Gradle Plugin Version >= $minimumRequiredAgpVersion.
            Found $currentAgpVersion
            """.trimIndent()
        )
    }

    object AndroidStyleSourceDirUsageWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(androidStyleSourceDirInUse: String, kotlinStyleSourceDirToUse: String) = build {
            name {
                "Deprecated 'Android Style' Source Directory"
            }
            message {
                """
                Usage of 'Android Style' source directory $androidStyleSourceDirInUse is deprecated.
                Use $kotlinStyleSourceDirToUse instead.
                
                To suppress this warning: put the following in your gradle.properties:
                ${PropertiesProvider.PropertyNames.KOTLIN_MPP_ANDROID_SOURCE_SET_LAYOUT_ANDROID_STYLE_NO_WARN}=true
                """.trimIndent()
            }
            documentation("https://kotl.in/android-source-set-layout-v2") { url ->
                "Learn more: $url"
            }
        }
    }

    object SourceSetLayoutV1StyleDirUsageWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(v1StyleSourceDirInUse: String, currentLayoutName: String, v2StyleSourceDirToUse: String) = build(
            "Deprecated Source Set Layout V1",
            """
            Found used source directory $v1StyleSourceDirInUse
            This source directory was supported by: ${multiplatformAndroidSourceSetLayoutV1.name}
            Current KotlinAndroidSourceSetLayout: $currentLayoutName
            New source directory is: $v2StyleSourceDirToUse
            """.trimIndent()
        )
    }

    object IncompatibleGradleVersionTooLowFatalError : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(
            currentGradleVersion: GradleVersion,
            minimallySupportedGradleVersion: GradleVersion,
        ) = build {
            name {
                "Gradle Version Incompatible with Kotlin Gradle Plugin"
            }
            message {
                """
                Kotlin Gradle Plugin <-> Gradle compatibility issue:
                The applied Kotlin Gradle is not compatible with the used Gradle version ($currentGradleVersion).
                """.trimIndent()
            }
            solution {
                "Please update the Gradle version to at least $minimallySupportedGradleVersion."
            }
        }
    }

    object IncompatibleAgpVersionTooLowFatalError : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(
            androidGradlePluginVersionString: String,
            minSupported: String,
        ) = build {
            name {
                "Android Gradle Plugin Version Incompatible with Kotlin Gradle Plugin"
            }
            message {
                """
                Kotlin Gradle Plugin <-> Android Gradle Plugin compatibility issue:
                The applied Android Gradle Plugin version ($androidGradlePluginVersionString) is lower than the minimum supported $minSupported.
                """.trimIndent()
            }
            solution {
                "Please update the Android Gradle Plugin version to at least $minSupported."
            }
        }
    }

    object FailedToGetAgpVersionWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(agpPluginId: String) = build {
            name {
                "Failed to Retrieve Android Gradle Plugin Version"
            }
            message {
                "Failed to get Android Gradle Plugin version (for '$agpPluginId' plugin)."
            }
            documentation("https://kotl.in/issue") { url ->
                "Please report a new Kotlin issue via $url."
            }
        }
    }

    object AndroidSourceSetLayoutV1SourceSetsNotFoundError : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(nameOfRequestedSourceSet: String) = build {
            name {
                "Renamed Android Source Set Not Found"
            }
            message {
                """
                KotlinSourceSet with name '$nameOfRequestedSourceSet' not found:
                The SourceSet requested ('$nameOfRequestedSourceSet') was renamed in Kotlin 1.9.0
                
                In order to migrate you might want to replace:
                sourceSets.getByName("androidTest") -> sourceSets.getByName("androidUnitTest")
                sourceSets.getByName("androidAndroidTest") -> sourceSets.getByName("androidInstrumentedTest")
                """.trimIndent()
            }
            documentation("https://kotl.in/android-source-set-layout-v2") { url ->
                "Learn more about the new Kotlin/Android SourceSet Layout: $url"
            }
        }
    }

    object KotlinJvmMainRunTaskConflict : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(targetName: String, taskName: String) = build(
            "JVM Main Run Task Conflict",
            "Target '$targetName': Unable to create run task '$taskName' as there is already such a task registered"
        )
    }

    object DeprecatedPropertyWithReplacement : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(deprecatedPropertyName: String, replacement: String) = build {
            name {
                "Deprecated Project Property '$deprecatedPropertyName'"
            }
            message {
                "Project property '$deprecatedPropertyName' is deprecated."
            }
            solution {
                "Please use '$replacement' instead."
            }
        }
    }

    object UnrecognizedKotlinNativeDistributionType : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(actualValue: String) = build {
            name {
                "Unrecognized Kotlin/Native Distribution Type"
            }
            message {
                "Gradle Property `kotlin.native.distribution.type` sets unknown Kotlin/Native distribution type: $actualValue"
            }
            solution {
                "Available values: `prebuilt`, `light`"
            }
        }
    }

    object AndroidTargetIsMissing : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(projectName: String, projectPath: String, androidPluginId: String) = build {
            name {
                "Missing `androidTarget()` in Kotlin Multiplatform Project"
            }
            message {
                """
                Missing `androidTarget()` Kotlin target in multiplatform project '$projectName ($projectPath)'.
                The Android Gradle plugin was applied without creating a corresponding `android()` Kotlin Target
                ```
                plugins {
                    id("$androidPluginId")
                    kotlin("multiplatform")
                }
                
                kotlin {
                    androidTarget() // <-- please register this Android target
                }
                ```
                """.trimIndent()
            }
        }
    }

    object AndroidGradlePluginIsMissing : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(trace: Throwable? = null) = build(throwable = trace) {
            name {
                "Missing Android Gradle Plugin"
            }
            message {
                """
                The Android target requires a 'Android Gradle Plugin' to be applied to the project.
                ```
                plugins {
                    kotlin("multiplatform")
                
                    /* Android Gradle Plugin missing */
                    id("com.android.library") /* <- Android Gradle Plugin for libraries */
                    id("com.android.application") <* <- Android Gradle Plugin for applications */
                }
                
                kotlin {
                    androidTarget() /* <- requires Android Gradle Plugin to be applied */
                }
                ```
                """.trimIndent()
            }
        }
    }

    object NoKotlinTargetsDeclared : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(projectName: String, projectPath: String) = build {
            name {
                "No Kotlin Targets Declared"
            }
            message {
                "Please initialize at least one Kotlin target in '${projectName} (${projectPath})'."
            }
            documentation("https://kotl.in/set-up-targets") { url ->
                "Read more $url"
            }
        }
    }

    object DisabledCinteropsCommonizationInHmppProject : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(affectedSourceSetsString: String, affectedCinteropsString: String) = build {
            name {
                "CInterop Commonization Disabled"
            }
            message {
                """
                The project is using Kotlin Multiplatform with hierarchical structure and disabled 'cinterop commonization'
                
                'cinterop commonization' can be enabled in your 'gradle.properties'
                kotlin.mpp.enableCInteropCommonization=true
                
                To hide this message, add to your 'gradle.properties'
                ${PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION}.nowarn=true 
            
                The following source sets are affected: 
                $affectedSourceSetsString
                
                The following cinterops are affected: 
                $affectedCinteropsString
                """.trimIndent()
            }
            documentation("https://kotlinlang.org/docs/mpp-share-on-platforms.html#use-native-libraries-in-the-hierarchical-structure") { url ->
                "See: $url"
            }
        }
    }

    object DisabledKotlinNativeTargets : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(disabledTargetNames: Collection<String>): ToolingDiagnostic = build {
            name {
                "Disabled Kotlin/Native Targets"
            }
            message {
                """
                The following Kotlin/Native targets cannot be built on this machine and are disabled:
                ${disabledTargetNames.joinToString()}
                """.trimIndent()
            }
            solution {
                "To hide this message, add '$KOTLIN_NATIVE_IGNORE_DISABLED_TARGETS=true' to the Gradle properties."
            }
        }
    }

    object InconsistentTargetCompatibilityForKotlinAndJavaTasks : ToolingDiagnosticFactory() {
        operator fun invoke(
            javaTaskName: String,
            targetCompatibility: String,
            kotlinTaskName: String,
            jvmTarget: String,
            severity: ToolingDiagnostic.Severity,
        ) = build(severity = severity) {
            name {
                "Inconsistent JVM Target Compatibility Between Java and Kotlin Tasks"
            }
            message {
                """
                Inconsistent JVM-target compatibility detected for tasks '$javaTaskName' ($targetCompatibility) and '$kotlinTaskName' ($jvmTarget).
                ${if (severity == WARNING) "This will become an error in Gradle 8.0." else ""}
                
                Consider using JVM Toolchain: https://kotl.in/gradle/jvm/toolchain
                Learn more about JVM-target validation: https://kotl.in/gradle/jvm/target-validation 
                """.trimIndent()
            }
        }
    }

    abstract class JsLikeEnvironmentNotChosenExplicitly(
        private val environmentName: String,
        private val targetType: String,
    ) : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(availableEnvironments: List<String>) = build(
            "JS Environment Not Selected",
            """
                |Please choose a $environmentName environment to build distributions and run tests.
                |Not choosing any of them will be an error in the future releases.
                |kotlin {
                |    $targetType {
                |        // To build distributions for and run tests use one or several of:
                |        ${availableEnvironments.joinToString(separator = "\n        ")}
                |    }
                |}
            """.trimMargin()
        )
    }

    object JsEnvironmentNotChosenExplicitly : JsLikeEnvironmentNotChosenExplicitly("JavaScript", "js")

    object WasmJsEnvironmentNotChosenExplicitly : JsLikeEnvironmentNotChosenExplicitly("WebAssembly-JavaScript", "wasmJs")

    object WasmWasiEnvironmentNotChosenExplicitly : JsLikeEnvironmentNotChosenExplicitly("WebAssembly WASI", "wasmWasi")

    object PreHmppDependenciesUsedInBuild : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(dependencyName: String) = build {
            name {
                "Deprecated Legacy Mode Dependency"
            }
            message {
                "The dependency '$dependencyName' was published in the legacy mode. Support for such dependencies will be removed in the future."
            }
            documentation("https://kotl.in/0b5kn8") { url ->
                "See: $url"
            }
        }
    }

    object ExperimentalTryNextWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build {
            name {
                "Experimental 'kotlin.experimental.tryNext' Option Enabled"
            }
            message {
                "ATTENTION: 'kotlin.experimental.tryNext' is an experimental option enabled in the project for trying out the next Kotlin compiler language version only."
            }
            solution {
                "Please refrain from using it in production code and provide feedback to the Kotlin team for any issues encountered via https://kotl.in/issue"
            }
        }
    }

    object KotlinSourceSetTreeDependsOnMismatch : ToolingDiagnosticFactory(WARNING) {
        private fun diagnosticName() = "Invalid Source Set Dependency Across Trees"

        operator fun invoke(dependeeName: String, dependencyName: String) = build {
            name(::diagnosticName)
            message {
                "Kotlin Source Set '$dependeeName' can't depend on '$dependencyName' as they are from different Source Set Trees."
            }
            solution {
                "Please remove this dependency edge."
            }
        }

        operator fun invoke(dependents: Map<String, List<String>>, dependencyName: String) = build {
            name(::diagnosticName)
            message {
                """
                Following Kotlin Source Set groups can't depend on '$dependencyName' together as they belong to different Kotlin Source Set Trees.
                ${renderSourceSetGroups(dependents).indentLines(16)}
                """.trimIndent()
            }
            solution {
                "Please keep dependsOn edges only from one group and remove the others. "
            }
        }

        private fun renderSourceSetGroups(sourceSetGroups: Map<String, List<String>>) = buildString {
            for ((sourceSetTreeName, sourceSets) in sourceSetGroups) {
                appendLine("Source Sets from '$sourceSetTreeName' Tree:")
                appendLine(sourceSets.joinToString("\n") { "  * '$it'" })
            }
        }
    }

    object KotlinSourceSetDependsOnDefaultCompilationSourceSet : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(dependeeName: String, dependencyName: String) = build {
            name {
                "Invalid Dependency on Default Compilation Source Set"
            }
            message {
                """
                Kotlin Source Set '$dependeeName' can't depend on '$dependencyName' which is a default source set for compilation.
                None of source sets can depend on the compilation default source sets.
                """.trimIndent()
            }
            solution {
                "Please remove this dependency edge."
            }
        }
    }

    object PlatformSourceSetConventionUsedWithCustomTargetName : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(sourceSet: KotlinSourceSet, target: KotlinTarget, expectedTargetName: String) =
            build(throwable = sourceSet.isAccessedByKotlinSourceSetConventionAt) {
                name {
                    "Source Set used with custom target name"
                }
                message {
                    """
                    Accessed '$sourceSet', but $expectedTargetName target used a custom name '${target.name}' (expected '$expectedTargetName'):
                    
                    Replace:
                    ```
                    kotlin {
                        $expectedTargetName("${target.name}") /* <- custom name used */
                    }
                    ```
                    
                    With:
                    ```
                    kotlin {
                        $expectedTargetName()
                    }
                    ```
                    """.trimIndent()
                }
            }
    }

    object PlatformSourceSetConventionUsedWithoutCorrespondingTarget : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(sourceSet: KotlinSourceSet, expectedTargetName: String) = build(
            "Source Set Used Without a Corresponding Target",
            """
                 |Accessed '$sourceSet' without the registering the $expectedTargetName target:
                 |  kotlin {
                 |      $expectedTargetName() /* <- register the '$expectedTargetName' target */
                 |
                 |      sourceSets.${sourceSet.name}.dependencies {
                 |
                 |      }
                 |  }
            """.trimMargin(),
            throwable = sourceSet.isAccessedByKotlinSourceSetConventionAt
        )
    }

    object AndroidMainSourceSetConventionUsedWithoutAndroidTarget : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(sourceSet: KotlinSourceSet) = build(
            "Android Source Set Used Without an Android Target",
            """
                |Accessed '$sourceSet' without registering the Android target
                |Please apply a given Android Gradle plugin (e.g. com.android.library) and register an Android target
                |
                |Example using the 'com.android.library' plugin:
                |
                |    plugins {
                |        id("com.android.library")
                |    }
                |
                |    android {
                |        namespace = "org.sample.library"
                |        compileSdk = 33
                |    }
                |
                |    kotlin {
                |        androidTarget() /* <- register the androidTarget */
                |    }
            """.trimMargin(),
            throwable = sourceSet.isAccessedByKotlinSourceSetConventionAt
        )
    }

    object IosSourceSetConventionUsedWithoutIosTarget : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(sourceSet: KotlinSourceSet) = build(
            "iOS Source Set Used Without an iOS Target",
            """
                |Accessed '$sourceSet' without registering any ios target:
                |  kotlin {
                |     /* Register at least one of the following targets */
                |     iosX64()
                |     iosArm64()
                |     iosSimulatorArm64()
                |
                |     /* Use convention */
                |     sourceSets.${sourceSet.name}.dependencies {
                |
                |     }
                |  }
            """.trimMargin(),
            throwable = sourceSet.isAccessedByKotlinSourceSetConventionAt
        )
    }

    object KotlinDefaultHierarchyFallbackDependsOnUsageDetected : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(project: Project, sourceSetsWithDependsOnEdges: Iterable<KotlinSourceSet>) = build {
            name {
                "Default Kotlin Hierarchy Template Not Applied Correctly"
            }
            message {
                """
                The Default Kotlin Hierarchy Template was not applied to '${project.displayName}':
                Explicit .dependsOn() edges were configured for the following source sets:
                ${sourceSetsWithDependsOnEdges.toSet().map { it.name }}
                
                Consider removing dependsOn-calls or disabling the default template by adding
                    '$KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE=false'
                to your gradle.properties
                """.trimIndent()
            }
            documentation("https://kotl.in/hierarchy-template") { url ->
                "Learn more about hierarchy templates: $url"
            }
        }
    }

    object KotlinDefaultHierarchyFallbackIllegalTargetNames : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(project: Project, illegalTargetNamesUsed: Iterable<String>) = build {
            name {
                "Default Kotlin Hierarchy Template Misconfiguration Due to Illegal Target Names"
            }
            message {
                """
                The Default Kotlin Hierarchy Template was not applied to '${project.displayName}':
                Source sets created by the following targets will clash with source sets created by the template:
                ${illegalTargetNamesUsed.toSet()}
                
                Consider renaming the targets or disabling the default template by adding
                    '$KOTLIN_MPP_APPLY_DEFAULT_HIERARCHY_TEMPLATE=false'
                to your gradle.properties
                """.trimIndent()
            }
            documentation("https://kotl.in/hierarchy-template") { url ->
                "Learn more about hierarchy templates: $url"
            }
        }
    }

    object XCFrameworkDifferentInnerFrameworksName : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(xcFramework: String, innerFrameworks: String) = build(
            "XCFramework Name Mismatch with Inner Frameworks",
            "Name of XCFramework '$xcFramework' differs from inner frameworks name '$innerFrameworks'! Framework renaming is not supported yet"
        )
    }

    object UnknownAppleFrameworkBuildType : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(envConfiguration: String) = build(
            "Unable to Detect Apple Framework Build Type",
            """
                Unable to detect Kotlin framework build type for CONFIGURATION=$envConfiguration automatically.
                Specify 'KOTLIN_FRAMEWORK_BUILD_TYPE' to 'debug' or 'release'
            """.trimIndent()
        )
    }

    object ExperimentalArtifactsDslUsed : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build {
            name {
                "Using Experimental 'kotlinArtifacts' DSL"
            }
            message {
                "'kotlinArtifacts' DSL is experimental and may be changed in the future."
            }
            solution {
                "To suppress this warning add '$KOTLIN_NATIVE_SUPPRESS_EXPERIMENTAL_ARTIFACTS_DSL_WARNING=true' to your gradle.properties"
            }
        }
    }

    private val presetsDeprecationSeverity = ToolingDiagnostic.Severity.ERROR

    object TargetFromPreset : ToolingDiagnosticFactory(presetsDeprecationSeverity) {
        const val DEPRECATION_MESSAGE = "The targetFromPreset() $PRESETS_DEPRECATION_MESSAGE_SUFFIX"
        operator fun invoke() = build(
            "targetFromPreset() Method Deprecated",
            DEPRECATION_MESSAGE
        )
    }

    object FromPreset : ToolingDiagnosticFactory(presetsDeprecationSeverity) {
        const val DEPRECATION_MESSAGE = "The fromPreset() $PRESETS_DEPRECATION_MESSAGE_SUFFIX"
        operator fun invoke() = build(
            "fromPreset() Function Deprecated",
            DEPRECATION_MESSAGE
        )
    }

    object CreateTarget : ToolingDiagnosticFactory(presetsDeprecationSeverity) {
        private const val DEPRECATION_MESSAGE = "The KotlinTargetPreset.createTarget() $PRESETS_DEPRECATION_MESSAGE_SUFFIX"
        operator fun invoke() = build(
            "KotlinTargetPreset.createTarget() Method Deprecated",
            DEPRECATION_MESSAGE
        )
    }

    object JvmWithJavaIsIncompatibleWithAndroid : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(androidPluginId: String, trace: Throwable?) = build(
            "`withJava()` in JVM Target Incompatible with Android Plugins",
            """
                'withJava()' is not compatible with Android Plugins
                Incompatible Android Plugin applied: '$androidPluginId'
                
                  kotlin {
                      jvm {
                          withJava() /* <- cannot be used when the Android Plugin is present */
                      }
                  }
            """.trimIndent(),
            throwable = trace
        )
    }

    abstract class KotlinTargetAlreadyDeclared(severity: ToolingDiagnostic.Severity) :
        ToolingDiagnosticFactory(severity) {
        operator fun invoke(targetDslFunctionName: String) = build {
            name {
                "`$targetDslFunctionName()` Kotlin Target Already Declared"
            }
            message {
                "Declaring multiple Kotlin Targets of the same type is not supported."
            }
            documentation("https://kotl.in/declaring-multiple-targets")
        }
    }

    object KotlinTargetAlreadyDeclaredWarning : KotlinTargetAlreadyDeclared(WARNING)
    object KotlinTargetAlreadyDeclaredError : KotlinTargetAlreadyDeclared(ERROR)

    object KotlinCompilationSourceDeprecation : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(trace: Throwable?) = build(throwable = trace) {
            name {
                "`KotlinCompilation.source(KotlinSourceSet)` Method Deprecated"
            }
            message {
                """
                `KotlinCompilation.source(KotlinSourceSet)` method is deprecated
                and will be removed in upcoming Kotlin releases.
                """.trimIndent()
            }
            documentation("https://kotl.in/compilation-source-deprecation") { url ->
                "See $url for details."
            }
        }
    }

    object CircularDependsOnEdges : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(sourceSetsOnCycle: Collection<String>) = build(
            "Circular dependsOn Relationship Detected in Kotlin Source Sets",
            "Circular dependsOn hierarchy found in the Kotlin source sets: ${sourceSetsOnCycle.joinToString(" -> ")}"
        )
    }

    object InternalKotlinGradlePluginPropertiesUsed : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(propertiesUsed: Collection<String>) = build(
            "Usage of Internal Kotlin Gradle Plugin Properties Detected",
            """
                |ATTENTION! This build uses the following Kotlin Gradle Plugin properties:
                |
                |${propertiesUsed.joinToString(separator = "\n")}
                |
                |Internal properties are not recommended for production use. 
                |Stability and future compatibility of the build is not guaranteed.
            """.trimMargin()
        )
    }

    object BuildToolsApiVersionInconsistency : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(expectedVersion: String, actualVersion: String?) = build(
            "Build Tools API Version Mismatch Detected",
            """
            Artifact $KOTLIN_MODULE_GROUP:$KOTLIN_BUILD_TOOLS_API_IMPL must have version aligned with the version of KGP when compilation via the Build Tools API is disabled.

            Expected version: $expectedVersion
            Actual resolved version: ${actualVersion ?: "not found"}
            """.trimIndent(),
        )
    }

    object WasmSourceSetsNotFoundError : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(nameOfRequestedSourceSet: String) = build {
            name {
                "Wasm Source Sets Missing Due to Renaming in Kotlin 1.9.20"
            }
            message {
                """
                KotlinSourceSet with name '$nameOfRequestedSourceSet' not found:
                The SourceSet requested ('$nameOfRequestedSourceSet') was renamed in Kotlin 1.9.20
                
                In order to migrate you might want to replace: 
                val wasmMain by getting -> val wasmJsMain by getting
                val wasmTest by getting -> val wasmJsTest by getting
                """.trimIndent()
            }
        }
    }

    object DuplicateSourceSetsError : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(duplicatedSourceSets: Map<String, List<String>>): ToolingDiagnostic {
            val duplicatesGroupsString = duplicatedSourceSets
                .map { entry -> entry.value.joinToString(", ") }
                .joinToString("], [", "[", "]")
            return build(
                "Duplicate Kotlin Source Sets Detected",
                "Duplicate Kotlin source sets have been detected: $duplicatesGroupsString." +
                        " Keep in mind that source set names are case-insensitive," +
                        " which means that `srcMain` and `sRcMain` are considered the same source set."
            )
        }
    }

    object CInteropRequiredParametersNotSpecifiedError : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke() = build {
            name {
                "CInterop Task Missing Required Parameters"
            }
            message {
                "For the Cinterop task, either the `definitionFile` or `packageName` parameter must be specified, however, neither has been provided."
            }
            documentation("https://kotlinlang.org/docs/multiplatform-dsl-reference.html#cinterops") { url ->
                "More info here: $url"
            }
        }
    }

    object IncorrectCompileOnlyDependencyWarning : ToolingDiagnosticFactory(WARNING) {

        data class CompilationDependenciesPair(
            val compilation: KotlinCompilation<*>,
            val dependencyCoords: List<String>,
        )

        operator fun invoke(
            compilationsWithCompileOnlyDependencies: List<CompilationDependenciesPair>,
        ): ToolingDiagnostic {

            val formattedPlatformNames = compilationsWithCompileOnlyDependencies
                .map { it.compilation.platformType.prettyName }
                .distinct()
                .sorted()
                .joinToString()

            val formattedCompileOnlyDeps = compilationsWithCompileOnlyDependencies
                .flatGroupBy(
                    keySelector = { it.dependencyCoords },
                    keyTransformer = { it },
                    valueTransformer = { it.compilation.defaultSourceSet.name },
                )
                .map { (dependency, sourceSetNames) ->
                    "$dependency (source sets: ${sourceSetNames.joinToString()})"
                }
                .distinct()
                .sorted()
                .joinToString("\n") { "    - $it" }

            return build(
                "Unsupported `compileOnly` Dependencies in Kotlin Targets",
                """
                |A compileOnly dependency is used in targets: $formattedPlatformNames.
                |Dependencies:
                |$formattedCompileOnlyDeps
                |
                |Using compileOnly dependencies in these targets is not currently supported, because compileOnly dependencies must be present during the compilation of projects that depend on this project.
                |
                |To ensure consistent compilation behaviour, compileOnly dependencies should be exposed as api dependencies.
                |
                |Example:
                |
                |    kotlin {
                |        sourceSets {
                |            nativeMain {
                |                dependencies {
                |                    compileOnly("org.example:lib:1.2.3")
                |                    // additionally add the compileOnly dependency as an api dependency:
                |                    api("org.example:lib:1.2.3")
                |                }
                |            }
                |        }
                |    }
                |
                |This warning can be suppressed in gradle.properties:
                |
                |    ${KOTLIN_SUPPRESS_GRADLE_PLUGIN_WARNINGS_PROPERTY}=${id}
                |
                """.trimMargin()
            )
        }
    }

    private const val BUG_REPORT_URL = "https://kotl.in/issue"
    private fun resourcesBugReportRequest(url: String) =
        "This is likely a bug in Kotlin Gradle Plugin configuration. Please report this issue to $url"

    object ResourcePublishedMoreThanOncePerTarget : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(targetName: String) = build {
            name {
                "Multiple Resource Publications Detected for Target '$targetName'"
            }
            message {
                "Only one resources publication per target $targetName is allowed."
            }
            documentation(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object AssetsPublishedMoreThanOncePerTarget : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke() = build {
            name {
                "Multiple Assets Publications Detected for Android Target"
            }
            message {
                "Only one assets publication per android target is allowed."
            }
            documentation(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object ResourceMayNotBePublishedForTarget : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(targetName: String) = build {
            name {
                "Resource Publication Not Supported for Target '$targetName'"
            }
            message {
                "Resources publication for target $targetName is not supported yet."
            }
            documentation(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object ResourceMayNotBeResolvedForTarget : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(targetName: String) = build {
            name {
                "Resource Resolution Not Supported for Target '$targetName'"
            }
            message {
                "Resources resolution for target $targetName is not supported."
            }
            documentation(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object ResourceMayNotBeResolvedWithGradleVersion : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(
            targetName: String, currentGradleVersion: String, minimumRequiredVersion: String,
        ) = build {
            name {
                "Resource Resolution for Target '$targetName' Requires Gradle $minimumRequiredVersion"
            }
            message {
                "Resources for target $targetName may not be resolved. Minimum required Gradle version is $minimumRequiredVersion but current is ${currentGradleVersion}."
            }
            documentation(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object UnknownValueProvidedForResourcesStrategy : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(value: String) = build(
            " Invalid Value Provided for '${PropertiesProvider.PropertyNames.KOTLIN_MPP_RESOURCES_RESOLUTION_STRATEGY}'",
            """
            Unknown value $value provided for ${PropertiesProvider.PropertyNames.KOTLIN_MPP_RESOURCES_RESOLUTION_STRATEGY}
            """.trimIndent()
        )
    }

    object MissingRuntimeDependencyConfigurationForWasmTarget : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(targetName: String) = build {
            name {
                "Missing Runtime Dependency Configuration for Wasm Target '$targetName'"
            }
            message {
                "Resources will not be resolved for $targetName as it is missing runtimeDependencyConfiguration."
            }
            documentation(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object MissingResourcesConfigurationForTarget : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(targetName: String) = build {
            name {
                "Missing Resource Configuration for Target '$targetName'"
            }
            message {
                "Resources will not be resolved for $targetName as it is missing resourcesConfiguration."
            }
            documentation(BUG_REPORT_URL, ::resourcesBugReportRequest)
        }
    }

    object DependencyDoesNotPhysicallyExist : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(dependency: File) = build {
            name {
                "Specified Dependency Does Not Exist"
            }
            message {
                "Unable to find the dependency at the location '${dependency.absolutePath}'."
            }
            solution {
                "Please make sure that the dependency exists at the specified location or ensure that dependency declarations are correct in your project."
            }
        }
    }

    object XcodeVersionTooHighWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(xcodeVersionString: String, maxTested: String) = build {
            name {
                "Xcode Version Too High for Kotlin Gradle Plugin"
            }
            message {
                """
                Kotlin <-> Xcode compatibility issue:
                The selected Xcode version ($xcodeVersionString) is higher than the maximum known to the Kotlin Gradle Plugin.
                Stability in such configuration hasn't been tested, please report encountered issues to https://kotl.in/issue
                
                Maximum tested Xcode version: $maxTested
                """.trimIndent()
            }
            solution {
                "To suppress this message add '${PropertiesProvider.PropertyNames.KOTLIN_APPLE_XCODE_COMPATIBILITY_NOWARN}=true' to your gradle.properties"
            }
        }
    }

    object ExperimentalFeatureWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(featureName: String, youtrackUrl: String) = build {
            name {
                "Experimental Feature Notice"
            }
            message {
                """                
                $featureName is an experimental feature and subject to change in any future releases.
                It may not function as you expect and you may encounter bugs. Use it at your own risk.
                """.trimIndent()
            }
            documentation(youtrackUrl) { url ->
                "Thank you for your understanding. We would appreciate your feedback on this feature in YouTrack $url."
            }
        }
    }

    object DeprecatedGradleProperties : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(usedDeprecatedProperty: String) = build {
            name {
                "Deprecated Gradle Property '$usedDeprecatedProperty' Used"
            }
            message {
                "The `$usedDeprecatedProperty` deprecated property is used in your build."
            }
            solution {
                "Please, stop using it as it is unsupported and may apply no effect to your build."
            }
        }
    }

    object RedundantDependsOnEdgesFound : ToolingDiagnosticFactory(WARNING) {
        data class RedundantEdge(val from: String, val to: String)

        operator fun invoke(redundantEdges: List<RedundantEdge>) = build {
            name {
                "Redundant dependsOn Kotlin Source Sets found"
            }
            message {
                val redundantEdgesString = buildString {
                    redundantEdges.forEach { edge -> appendLine(" * ${edge.from}.dependsOn(${edge.to})") }
                }

                """
                |Redundant dependsOn edges between Kotlin Source Sets found.
                |Please remove the following dependsOn invocations from your build scripts:
                |$redundantEdgesString
                """.trimMargin()
            }
            documentation("https://kotl.in/hierarchy-template") { url ->
                "They are already added from Kotlin Target Hierarchy template $url"
            }
        }
    }

    object BrokenKotlinNativeBundleError : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(kotlinNativeHomePropertyValue: String?, kotlinNativeHomeProperty: String) =
            build {
                name {
                    "Kotlin/Native Distribution Missing Required Subdirectories"
                }
                message {
                    "The Kotlin/Native distribution ($kotlinNativeHomePropertyValue) used in this build does not provide required subdirectories."
                }
                solution {
                    "Make sure that the '$kotlinNativeHomeProperty' property points to a valid Kotlin/Native distribution."
                }
            }
    }

    object KonanHomeConflictDeclaration : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(konanDataDirPropertyValue: File?, kotlinNativeHomeProperty: String?): ToolingDiagnostic =
            build(
                "Both ${NativeProperties.KONAN_DATA_DIR.name} and ${NativeProperties.NATIVE_HOME.name} Properties Declared",
                """
                Both ${NativeProperties.KONAN_DATA_DIR.name}=${konanDataDirPropertyValue} and ${NativeProperties.NATIVE_HOME.name}=${kotlinNativeHomeProperty} are declared.
                The ${NativeProperties.KONAN_DATA_DIR.name}=${konanDataDirPropertyValue} path will be given the highest priority.
                """.trimIndent()
            )
    }

    object NoComposeCompilerPluginAppliedWarning : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke() = build {
            name {
                "Compose Compiler Plugin Not Applied"
            }
            message {
                "The Compose compiler plugin is now a part of Kotlin, please apply the 'org.jetbrains.kotlin.plugin.compose' Gradle plugin to enable it."
            }
            documentation("https://kotl.in/compose-plugin") { url ->
                "Learn more about this at $url"
            }
        }
    }

    object DeprecatedJvmHistoryBasedIncrementalCompilationDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(): ToolingDiagnostic = build {
            name {
                "History-Based Incremental Compilation Deprecated for JVM Platform"
            }
            message {
                "History based incremental compilation approach for JVM platform is deprecated and will be removed" +
                        " soon in favor of approach based on ABI snapshots."
            }
            solution {
                "Please remove '${PropertiesProvider.PropertyNames.KOTLIN_INCREMENTAL_USE_CLASSPATH_SNAPSHOT}=false' from 'gradle.properties' file."
            }
        }
    }

    object DeprecatedInKMPJavaPluginsDiagnostic : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(pluginId: String): ToolingDiagnostic {
            val pluginString = when (pluginId) {
                "application" -> "'$pluginId' (also applies 'java' plugin)"
                "java-library" -> "'$pluginId' (also applies 'java' plugin)"
                else -> "'$pluginId'"
            }

            return build {
                name {
                    "'$pluginId' Plugin Incompatible with 'org.jetbrains.kotlin.multiplatform' Plugin"
                }
                message {
                    "$pluginString Gradle plugin is not compatible with 'org.jetbrains.kotlin.multiplatform' plugin."
                }
                solution {
                    "Consider adding a new subproject with '$pluginId' plugin where the KMP project is added as a dependency."
                }
            }
        }
    }

    object XcodeUserScriptSandboxingDiagnostic : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(userScriptSandboxingEnabled: Boolean) = build {
            name {
                "User Script Sandboxing Enabled in Xcode Project"
            }
            message {
                """
                ${if (userScriptSandboxingEnabled) "You" else "BUILT_PRODUCTS_DIR is not accessible, probably you"} have sandboxing for user scripts enabled.
                
                In your Xcode project, navigate to "Build Setting",
                and under "Build Options" set "User script sandboxing" (ENABLE_USER_SCRIPT_SANDBOXING) to "NO".
                Then, run "./gradlew --stop" to stop the Gradle daemon
                """.trimIndent()
            }
            documentation("https://kotl.in/iq4uke") { url ->
                "For more information, see documentation: $url"
            }
        }
    }

    object UnsupportedTargetShortcutError : ToolingDiagnosticFactory(ERROR) {
        operator fun invoke(shortcutName: String, explicitTargets: String, trace: Throwable) = build(throwable = trace) {
            name {
                "'$shortcutName' Target Shortcut Deprecated and Unsupported"
            }
            message {
                """
                The $shortcutName target shortcut is deprecated and no longer supported.
                Please explicitly declare your targets using:
                
                """.trimIndent() + explicitTargets
            }
            documentation("https://kotl.in/6ixl2f") { url ->
                "For a complete list of supported targets, refer to the documentation: $url"
            }
        }
    }

    object AndroidPublicationNotConfigured : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(
            componentName: String,
            publicationName: String,
        ): ToolingDiagnostic = build(throwable = Throwable()) {
            name {
                "Android Publication '$publicationName' Misconfigured for Variant '$componentName'"
            }
            message {
                """
                Android Publication '$publicationName' for variant '$componentName' was not configured properly:
                
                To avoid this warning, please create and configure Android publication variant with name '$componentName'.
                Example:
                ```
                android {
                    publishing {
                        singleVariant("$componentName") {}
                    }
                }
                ```
                """.trimIndent()
            }
            documentation("https://kotl.in/oe70nr")
        }
    }

    object KotlinCompilerEmbeddableIsPresentInClasspath : ToolingDiagnosticFactory(WARNING) {
        operator fun invoke(): ToolingDiagnostic = build {
            name {
                "'org.jetbrains.kotlin:kotlin-compiler-embeddable' Artifact Present in Build Classpath"
            }
            message {
                """
                The artifact `org.jetbrains.kotlin:kotlin-compiler-embeddable` is present in the build classpath along Kotlin Gradle plugin.
                This may lead to unpredictable and inconsistent behavior.
                """.trimIndent()
            }
            documentation("https://kotl.in/gradle/internal-compiler-symbols")
        }
    }

    object NotCompatibleWithGradle9 : ToolingDiagnosticFactory(FATAL) {
        operator fun invoke(
            fixAction: String
        ): ToolingDiagnostic = build {
            name {
                "Kotlin Gradle Plugin Not Compatible with Gradle 9"
            }
            message {
                "Current configuration of Kotlin Gradle Plugin is not compatible with Gradle 9."
            }
            solution {
                "Please $fixAction to fix it."
            }
        }
    }
}

private fun String.indentLines(nSpaces: Int = 4, skipFirstLine: Boolean = true): String {
    val spaces = String(CharArray(nSpaces) { ' ' })

    return lines()
        .withIndex()
        .joinToString(separator = "\n") { (index, line) ->
            if (skipFirstLine && index == 0) return@joinToString line
            if (line.isNotBlank()) "$spaces$line" else line
        }
}
