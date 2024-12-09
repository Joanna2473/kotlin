// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB
// API_VERSION: 2.2
// EXPLICIT_API_MODE: STRICT

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
public value class UIntWrapper
/* error */<!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>constructor(
    /* @get: error */ <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public val u: UInt<!>
)<!>

@JvmExposeBoxed
@JvmInline
public value class IC /* error */ <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>(public val s: String)<!>

@JvmInline
public value class ICC
/* error */ <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>constructor(
    public val s: String
)<!> {
    <!JVM_EXPOSE_BOXED_METHOD_MUST_BE_EXPLICIT!>public fun method() {}<!> // error

    <!JVM_EXPOSE_BOXED_METHOD_MUST_BE_EXPLICIT!>public val property: Int // error
        <!JVM_EXPOSE_BOXED_METHOD_MUST_BE_EXPLICIT!>get() = TODO()<!><!>
}

public fun returnsResult(): Result<Any> = TODO() // OK

public fun acceptsResult(result: Result<Any>) {} // OK

public fun globalReturnsIC(): UInt = TODO() // OK

<!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public fun acceptsIC(ic: IC) {}<!> // Error

public val withGetter: IC = TODO() // OK

<!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public var withSetter: IC = TODO()<!> // Error

public class Foo
/* error */<!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public constructor(
    /* @get: error */<!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public val ic: IC<!>
)<!> {
    public fun returnsResult(): Result<Any> = TODO() // OK

    public fun acceptsResult(result: Result<Any>) {} // OK

    <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public fun globalReturnsIC(): UInt = TODO()<!> // Error

    <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public fun acceptsIC(ic: IC) {}<!> // Error

    <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public val withGetter: IC = TODO()<!> // Error

    <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT, JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>public var withSetter: IC = TODO()<!> // Error
}

@JvmExposeBoxed
public class Foo2 /* error */ <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>(ic: IC)<!>

public class Foo3 /* error */ <!JVM_EXPOSE_BOXED_MUST_BE_EXPLICIT!>(ic: IC)<!>

public suspend fun suspendMe(ic: IC) {}
