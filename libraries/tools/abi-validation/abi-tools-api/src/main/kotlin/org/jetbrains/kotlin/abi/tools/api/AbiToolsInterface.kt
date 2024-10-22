/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

/**
 * All features of Kotlin ABI Validation tool.
 *
 * @since 2.1.20
 */
interface AbiToolsInterface {
    /**
     * A set of features for working with legacy format dumps, used in previos [Binary Compatibility Validator plugin](https://github.com/Kotlin/binary-compatibility-validator).
     */
    val legacy: LegacyAbiToolsInterface
}