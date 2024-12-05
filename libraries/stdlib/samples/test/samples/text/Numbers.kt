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
    fun toByte() {
        assertPrints("0".toByte(), "0")
        assertPrints("42".toByte(), "42")
        assertPrints("042".toByte(), "42")
        assertPrints("-42".toByte(), "-42")
        // Byte.MAX_VALUE
        assertPrints("127".toByte(), "127")
        // Byte overflow
        assertFailsWith<NumberFormatException> { "128".toByte() }
        // 'a' is not a digit
        assertFailsWith<NumberFormatException> { "-1a".toByte() }
        // underscore
        assertFailsWith<NumberFormatException> { "1_00".toByte() }
        // whitespaces
        assertFailsWith<NumberFormatException> { " 22 ".toByte() }
    }

    @Sample
    fun toShort() {
        assertPrints("0".toShort(), "0")
        assertPrints("42".toShort(), "42")
        assertPrints("042".toShort(), "42")
        assertPrints("-42".toShort(), "-42")
        // Short.MAX_VALUE
        assertPrints("32767".toShort(), "32767")
        // Short overflow
        assertFailsWith<NumberFormatException> { "32768".toShort() }
        // 'a' is not a digit
        assertFailsWith<NumberFormatException> { "-1a".toShort() }
        // underscore
        assertFailsWith<NumberFormatException> { "1_000".toShort() }
        // whitespaces
        assertFailsWith<NumberFormatException> { " 1000 ".toShort() }
    }

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

    @Sample
    fun toLong() {
        assertPrints("0".toLong(), "0")
        assertPrints("42".toLong(), "42")
        assertPrints("042".toLong(), "42")
        assertPrints("-42".toLong(), "-42")
        // Long.MAX_VALUE
        assertPrints("9223372036854775807".toLong(), "9223372036854775807")
        // Long overflow
        assertFailsWith<NumberFormatException> { "9223372036854775808".toLong() }
        // 'a' is not a digit
        assertFailsWith<NumberFormatException> { "-1a".toLong() }
        // underscore
        assertFailsWith<NumberFormatException> { "1_000".toLong() }
        // whitespaces
        assertFailsWith<NumberFormatException> { " 1000 ".toLong() }
    }
}