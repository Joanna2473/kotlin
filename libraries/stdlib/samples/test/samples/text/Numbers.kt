/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.text

import samples.Sample
import samples.assertPrints
import kotlin.test.assertFailsWith

class Numbers {

    @Sample
    fun toInt() {
        assertPrints("0".toInt(), "0")
        assertPrints("42".toInt(), "42")
        assertPrints("042".toInt(), "42")
        assertPrints("-42".toInt(), "-42")
        // Int.MAX_VALUE
        assertPrints("2147483647".toInt(), "2147483647")
        // Int overflow
        assertFailsWith<NumberFormatException> { "2147483648".toInt() }
        // 'a' is not a digit
        assertFailsWith<NumberFormatException> { "-1a".toInt() }
        // underscore
        assertFailsWith<NumberFormatException> { "1_000".toInt() }
        // whitespaces
        assertFailsWith<NumberFormatException> { " 1000 ".toInt() }
    }
}