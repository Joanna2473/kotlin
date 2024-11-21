// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ExpectedTypeGuidedResolution

enum class A {
    A1, A2
}
enum class B {
    B1, B2
}

fun foo(a: A): Int = 1
fun foo(b: B): Int = 2

val result = foo(A1) + foo(B2)