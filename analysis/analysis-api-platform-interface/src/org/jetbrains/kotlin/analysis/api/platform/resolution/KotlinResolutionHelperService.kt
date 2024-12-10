/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.resolution

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent

/**
 * This service provides a bridge between the Analysis API, Kotlin IntelliJ plugin and Java IntelliJ plugin.
 *
 * In particular, this service helps to mark periods when the thread is busy with Kotlin resolution. During this time
 * more strict rules may be applied on Java resolution, so it should take into account this to avoid Kotlin compiler contract violations.
 *
 * The service use site guarantees that all [enterBlockWithCompilerContractChecks] and [exitBlockWithCompilerContractChecks] calls are paired,
 * so for each [enterBlockWithCompilerContractChecks] call where will be the following [exitBlockWithCompilerContractChecks]. **Nested calls are allowed**.
 */
@KaIdeApi
public interface KotlinResolutionHelperService : KotlinOptionalPlatformComponent {
    /**
     * This method is called before Kotlin lazy resolution.
     */
    @KaImplementationDetail
    public fun enterBlockWithCompilerContractChecks()

    /**
     * This method is called after Kotlin lazy resolution.
     */
    @KaImplementationDetail
    public fun exitBlockWithCompilerContractChecks()

    /**
     * Whether the thread is inside the Kotlin resolver
     */
    public val isInsideKotlinResolver: Boolean

    @KaIdeApi
    public companion object {
        public fun getInstance(project: Project): KotlinResolutionHelperService? = project.serviceOrNull<KotlinResolutionHelperService>()
    }
}
