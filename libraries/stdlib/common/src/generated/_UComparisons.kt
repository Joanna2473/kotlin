/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("UComparisonsKt")

package kotlin.comparisons

//
// NOTE: THIS FILE IS AUTO-GENERATED by the GenerateStandardLib.kt
// See: https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib
//

import kotlin.contracts.*
import kotlin.random.*

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.5")
public fun maxOf(a: UInt, b: UInt): UInt {
    return if (a >= b) a else b
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.5")
public fun maxOf(a: ULong, b: ULong): ULong {
    return if (a >= b) a else b
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.5")
public fun maxOf(a: UByte, b: UByte): UByte {
    return if (a >= b) a else b
}

/**
 * Returns the greater of two values.
 */
@SinceKotlin("1.5")
public fun maxOf(a: UShort, b: UShort): UShort {
    return if (a >= b) a else b
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: UInt, b: UInt, c: UInt): UInt {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: ULong, b: ULong, c: ULong): ULong {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: UByte, b: UByte, c: UByte): UByte {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the greater of three values.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun maxOf(a: UShort, b: UShort, c: UShort): UShort {
    return maxOf(a, maxOf(b, c))
}

/**
 * Returns the greater of the given values.
 */
@SinceKotlin("1.4")
@ExperimentalUnsignedTypes
public fun maxOf(a: UInt, vararg other: UInt): UInt {
    var max = a
    for (e in other) max = maxOf(max, e)
    return max
}

/**
 * Returns the greater of the given values.
 */
@SinceKotlin("1.4")
@ExperimentalUnsignedTypes
public fun maxOf(a: ULong, vararg other: ULong): ULong {
    var max = a
    for (e in other) max = maxOf(max, e)
    return max
}

/**
 * Returns the greater of the given values.
 */
@SinceKotlin("1.4")
@ExperimentalUnsignedTypes
public fun maxOf(a: UByte, vararg other: UByte): UByte {
    var max = a
    for (e in other) max = maxOf(max, e)
    return max
}

/**
 * Returns the greater of the given values.
 */
@SinceKotlin("1.4")
@ExperimentalUnsignedTypes
public fun maxOf(a: UShort, vararg other: UShort): UShort {
    var max = a
    for (e in other) max = maxOf(max, e)
    return max
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.5")
public fun minOf(a: UInt, b: UInt): UInt {
    return if (a <= b) a else b
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.5")
public fun minOf(a: ULong, b: ULong): ULong {
    return if (a <= b) a else b
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.5")
public fun minOf(a: UByte, b: UByte): UByte {
    return if (a <= b) a else b
}

/**
 * Returns the smaller of two values.
 */
@SinceKotlin("1.5")
public fun minOf(a: UShort, b: UShort): UShort {
    return if (a <= b) a else b
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun minOf(a: UInt, b: UInt, c: UInt): UInt {
    return minOf(a, minOf(b, c))
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun minOf(a: ULong, b: ULong, c: ULong): ULong {
    return minOf(a, minOf(b, c))
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun minOf(a: UByte, b: UByte, c: UByte): UByte {
    return minOf(a, minOf(b, c))
}

/**
 * Returns the smaller of three values.
 */
@SinceKotlin("1.5")
@kotlin.internal.InlineOnly
public inline fun minOf(a: UShort, b: UShort, c: UShort): UShort {
    return minOf(a, minOf(b, c))
}

/**
 * Returns the smaller of the given values.
 */
@SinceKotlin("1.4")
@ExperimentalUnsignedTypes
public fun minOf(a: UInt, vararg other: UInt): UInt {
    var min = a
    for (e in other) min = minOf(min, e)
    return min
}

/**
 * Returns the smaller of the given values.
 */
@SinceKotlin("1.4")
@ExperimentalUnsignedTypes
public fun minOf(a: ULong, vararg other: ULong): ULong {
    var min = a
    for (e in other) min = minOf(min, e)
    return min
}

/**
 * Returns the smaller of the given values.
 */
@SinceKotlin("1.4")
@ExperimentalUnsignedTypes
public fun minOf(a: UByte, vararg other: UByte): UByte {
    var min = a
    for (e in other) min = minOf(min, e)
    return min
}

/**
 * Returns the smaller of the given values.
 */
@SinceKotlin("1.4")
@ExperimentalUnsignedTypes
public fun minOf(a: UShort, vararg other: UShort): UShort {
    var min = a
    for (e in other) min = minOf(min, e)
    return min
}

