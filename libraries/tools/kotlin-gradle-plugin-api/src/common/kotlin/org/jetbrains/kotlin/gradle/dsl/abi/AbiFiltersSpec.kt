/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

import org.gradle.api.Action
import org.gradle.api.provider.SetProperty

public interface AbiFiltersSpec {
    val excluded: AbiFilterSetSpec
    val included: AbiFilterSetSpec

    fun excluded(action: Action<AbiFilterSetSpec>) {
        action.execute(excluded)
    }

    fun included(action: Action<AbiFilterSetSpec>) {
        action.execute(included)
    }
}

public interface AbiFilterSetSpec {
    val classes: SetProperty<String>

    val annotatedWith: SetProperty<String>
}
