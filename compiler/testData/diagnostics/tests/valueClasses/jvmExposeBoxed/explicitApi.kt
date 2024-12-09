// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB
// API_VERSION: 2.2
// EXPLICIT_API_MODE: STRICT

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
public value class UIntWrapper
/* error */constructor(
    /* @get: error */ public val u: UInt
)

@JvmExposeBoxed
@JvmInline
public value class IC /* error */ (public val s: String)

@JvmInline
public value class ICC
/* error */ constructor(
    public val s: String
) {
    public fun method() {} // error

    public val property: Int // error
        get() = TODO()
}

public fun returnsResult(): Result<Any> = TODO() // OK

public fun acceptsResult(result: Result<Any>) {} // OK

public fun globalReturnsIC(): UInt = TODO() // OK

public fun acceptsIC(ic: IC) {} // Error

public val withGetter: IC = TODO() // OK

public var withSetter: IC = TODO() // Error

public class Foo
/* error */public constructor(
    /* @get: error */public val ic: IC
) {
    public fun returnsResult(): Result<Any> = TODO() // OK

    public fun acceptsResult(result: Result<Any>) {} // OK

    public fun globalReturnsIC(): UInt = TODO() // Error

    public fun acceptsIC(ic: IC) {} // Error

    public val withGetter: IC = TODO() // Error

    public var withSetter: IC = TODO() // Error
}

@JvmExposeBoxed
public class Foo2 /* error */ (ic: IC)

public class Foo3 /* error */ (ic: IC)

public suspend fun suspendMe(ic: IC) {}
