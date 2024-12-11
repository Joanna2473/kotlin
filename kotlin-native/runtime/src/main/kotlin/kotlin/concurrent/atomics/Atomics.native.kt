/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.concurrent.atomics

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.concurrent.*
import kotlin.concurrent.atomicGetField
import kotlin.concurrent.atomicSetField
import kotlin.concurrent.getAndSetField
import kotlin.native.internal.*
import kotlin.reflect.KMutableProperty0

/**
 * An [Int] value that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange], [fetchAndAdd], [addAndFetch],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public actual class AtomicInt public actual constructor(@Volatile private var value: Int) {
    /**
     * Atomically loads the value from this [AtomicInt].
     */
    public actual fun load(): Int = this::value.atomicGetField()

    /**
     * Atomically stores the [new value][newValue] into this [AtomicInt].
     */
    public actual fun store(newValue: Int): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the [new value][newValue] into this [AtomicInt] and returns the old value.
     */
    public actual fun exchange(newValue: Int): Int = this::value.getAndSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expectedValue: Int, newValue: Int): Boolean = this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicInt] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expectedValue: Int, newValue: Int): Int = this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the old value.
     */
    public actual fun fetchAndAdd(delta: Int): Int = this::value.getAndAddField(delta)

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicInt] and returns the new value.
     */
    public actual fun addAndFetch(delta: Int): Int = this::value.getAndAddField(delta) + delta

    /**
     * Returns the string representation of the [Int] value stored in this [AtomicInt].
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * A [Long] value that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange], [fetchAndAdd], [addAndFetch],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public actual class AtomicLong public actual constructor(@Volatile private var value: Long) {
    /**
     * Atomically loads the value from this [AtomicLong].
     */
    public actual fun load(): Long = this::value.atomicGetField()

    /**
     * Atomically stores the [new value][newValue] into this [AtomicLong].
     */
    public actual fun store(newValue: Long): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the [new value][newValue] into this [AtomicLong] and returns the old value.
     */
    public actual fun exchange(newValue: Long): Long = this::value.getAndSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expectedValue: Long, newValue: Long): Boolean = this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicLong] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expectedValue: Long, newValue: Long): Long = this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the old value.
     */
    public actual fun fetchAndAdd(delta: Long): Long = this::value.getAndAddField(delta)

    /**
     * Atomically adds the [given value][delta] to the current value of this [AtomicLong] and returns the new value.
     */
    public actual fun addAndFetch(delta: Long): Long = this::value.getAndAddField(delta) + delta

    /**
     * Returns the string representation of the underlying [Long] value.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * A [Boolean] value that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public actual class AtomicBoolean actual constructor(@Volatile private var value: Boolean) {

    /**
     * Atomically loads the value from this [AtomicBoolean].
     */
    public actual fun load(): Boolean = this::value.atomicGetField()

    /**
     * Atomically stores the [new value][newValue] into this [AtomicBoolean].
     */
    public actual fun store(newValue: Boolean): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] and returns the old value.
     */
    public actual fun exchange(newValue: Boolean): Boolean = this::value.getAndSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndSet(expectedValue: Boolean, newValue: Boolean): Boolean = this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicBoolean] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by value.
     */
    public actual fun compareAndExchange(expectedValue: Boolean, newValue: Boolean): Boolean = this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Returns the string representation of the underlying [Boolean] value.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String = value.toString()
}

/**
 * An object reference that may be updated atomically.
 *
 * Read operation [load] has the same memory effects as reading a [Volatile] property;
 * Write operation [store] has the same memory effects as writing a [Volatile] property;
 * Read-modify-write operations, like [exchange], [compareAndSet], [compareAndExchange],
 * have the same memory effects as reading and writing a [Volatile] property.
 *
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 */
@SinceKotlin("2.1")
@ExperimentalStdlibApi
public actual class AtomicReference<T> actual constructor(@Volatile private var value: T) {

    /**
     * Atomically loads the value from this [AtomicReference].
     */
    public actual fun load(): T = this::value.atomicGetField()

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] and returns the old value.
     */
    public actual fun exchange(newValue: T): T = this::value.getAndSetField(newValue)

    /**
     * Atomically stores the [new value][newValue] into this [AtomicReference].
     */
    public actual fun store(newValue: T): Unit = this::value.atomicSetField(newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * This operation has so-called strong semantics,
     * meaning that it returns false if and only if current and expected values are not equal.
     *
     * Comparison of values is done by reference.
     */
    public actual fun compareAndSet(expectedValue: T, newValue: T): Boolean = this::value.compareAndSetField(expectedValue, newValue)

    /**
     * Atomically stores the given [new value][newValue] into this [AtomicReference] if the current value equals the [expected value][expectedValue]
     * and returns the old value in any case.
     *
     * Comparison of values is done by reference.
     */
    public actual fun compareAndExchange(expectedValue: T, newValue: T): T = this::value.compareAndExchangeField(expectedValue, newValue)

    /**
     * Returns the string representation of the underlying object.
     *
     * This operation does not provide any atomicity guarantees.
     */
    public actual override fun toString(): String =
            "${debugString(this)} -> ${debugString(value)}"
}

/**
 * A [kotlinx.cinterop.NativePtr] value that is always updated atomically.
 * For additional details about atomicity guarantees for reads and writes see [kotlin.concurrent.Volatile].
 *
 * [kotlinx.cinterop.NativePtr] is a value type, hence it is stored in [AtomicNativePtr] without boxing
 * and [compareAndSet], [compareAndExchange] operations perform comparison by value.
 */
@SinceKotlin("2.1")
@ExperimentalForeignApi
public class AtomicNativePtr(@Volatile public var value: NativePtr) {
    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun getAndSet(newValue: NativePtr): NativePtr {
        // Pointer types are allowed for atomicrmw xchg operand since LLVM 15.0,
        // after LLVM version update, it may be implemented via getAndSetField intrinsic.
        // Check: https://youtrack.jetbrains.com/issue/KT-57557
        while (true) {
            val old = value
            if (this::value.compareAndSetField(old, newValue)) {
                return old
            }
        }
    }

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndSet(expected: NativePtr, newValue: NativePtr): Boolean =
            this::value.compareAndSetField(expected, newValue)

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by value.
     */
    public fun compareAndExchange(expected: NativePtr, newValue: NativePtr): NativePtr =
            this::value.compareAndExchangeField(expected, newValue)

    /**
     * Returns the string representation of the current [value].
     */
    public override fun toString(): String = value.toString()
}

private fun idString(value: Any) = value.hashCode().toUInt().toString(16)

private fun debugString(value: Any?): String {
    if (value == null) return "null"
    return "${value::class.qualifiedName}: ${idString(value)}"
}

/**
 * Atomically gets the value of the field referenced by [this].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * This is equivalent to KMutableProperty0#get() invocation and used internally to optimize allocation of a property reference.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.ATOMIC_GET_FIELD)
internal external fun <T> KMutableProperty0<T>.atomicGetField(): T

/**
 * Atomically sets the value of the field referenced by [this] to the [new value][newValue].
 *
 * Provides sequential consistent ordering guarantees.
 *
 * This is equivalent to KMutableProperty0#set(value: T) invocation and used internally to optimize allocation of a property reference.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.ATOMIC_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.atomicSetField(newValue: T)

/**
 * Atomically sets the value of the field referenced by [this] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue].
 * Returns true if the operation was successful and false only if the current value of the field was not equal to the expected value.
 *
 * Comparison is done by reference or value depending on field representation.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.COMPARE_AND_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.compareAndSetField(expectedValue: T, newValue: T): Boolean

/**
 * Atomically sets the value of the field referenced by [this] to the [new value][newValue]
 * if the current value equals the [expected value][expectedValue] and returns the old value of the field in any case.
 *
 * Comparison is done by reference or value depending on field representation.
 *
 * Provides sequential consistent ordering guarantees and never fails spuriously.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.COMPARE_AND_EXCHANGE_FIELD)
internal external fun <T> KMutableProperty0<T>.compareAndExchangeField(expectedValue: T, newValue: T): T

/**
 * Atomically sets the value of the field referenced by [this] to the [new value][newValue] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_SET_FIELD)
internal external fun <T> KMutableProperty0<T>.getAndSetField(newValue: T): T

/**
 * Atomically adds the given [delta] to the value of the field referenced by [this] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Short>.getAndAddField(delta: Short): Short

/**
 * Atomically adds the given [delta] to the value of the field referenced by [this] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Int>.getAndAddField(delta: Int): Int

/**
 * Atomically adds the given [delta] to the value of the field referenced by [this] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Long>.getAndAddField(delta: Long): Long

/**
 * Atomically adds the given [delta] to the value of the field referenced by [this] and returns the old value of the field.
 *
 * Provides sequential consistent ordering guarantees.
 *
 * For now, it can be used only within the same file, where property is defined.
 * Check https://youtrack.jetbrains.com/issue/KT-55426 for details.
 *
 * If [this] is not a compile-time known reference to the property with [Volatile] annotation [IllegalArgumentException]
 * would be thrown.
 *
 * If property referenced by [this] has nontrivial setter it will not be called.
 */
@PublishedApi
@TypedIntrinsic(IntrinsicType.GET_AND_ADD_FIELD)
internal external fun KMutableProperty0<Byte>.getAndAddField(delta: Byte): Byte