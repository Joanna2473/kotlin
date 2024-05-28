// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: main
// FILE: main.kt
class Foo {
    inner class Inner {}

    class Nested {
        operator fun plus(other: Int): Nested = this
    }
}

interface MyInterface

fun Foo.ext() {}

inline fun foo() {}

// FILE: packaged.kt
package a.b.c

abstract class A

enum class E {
    A, B, C
}
