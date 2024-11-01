/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

public class AbiFilters(
    public val includedClasses: Set<String>,
    public val excludedClasses: Set<String>,
    public val includedAnnotatedWith: Set<String>,
    public val excludedAnnotatedWith: Set<String>
)
