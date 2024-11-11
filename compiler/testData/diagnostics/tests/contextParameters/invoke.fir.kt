// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -UNSUPPORTED_FEATURE, -CONTEXT_RECEIVERS_DEPRECATED

fun String.foo(f: context(String) () -> Unit = {}) {
    f()
    f("") // explicit argument should still work
}

fun foo(f: context(String) () -> Unit = {}) {
    f("") // explicit argument should still work
}

interface Ctx
interface CtxA : Ctx
interface CtxB : Ctx

context(a: CtxA, b: CtxB)
fun bar(f: context(Ctx) () -> Unit) {
    <!MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER!>f<!><!NO_VALUE_FOR_PARAMETER!>()<!>
    f(a) // explicit argument should still work
    f(b) // explicit argument should still work
}

context(_: String)
fun baz(param: String, f: context(String, Int) (String) -> Unit){
    with(1) {
        f(param)
    }
}

fun <T> context(t: T, f: context(T) () -> Unit) = f(t)

fun qux(
    f1: context(String, Int) () -> Unit,
    f2: context(String, Int) (Boolean) -> Unit,
    f3: context(String, Int) Boolean.() -> Unit,
    f4: context(String, Int) String.() -> Unit,
    f5: context(String, Int) String.(Boolean) -> Unit,
) {
    f1("", 1)
    f2("", 1, true)
    f3("", 1, true)
    f4("", 1, "")
    f5("", 1, "", true)

    with("") {
        with(1) {
            f1()
            f2(true)
            f3(true)
            with(true) {
                f3()
            }
            true.f3()
            f4()
            f4("")
            "".f4()
            f5<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>
            f5(true)
            f5("", true)
            "".f5(true)
        }
    }

    context("") {
        with(1) {
            f1()
            f2(true)
        }
    }

    context("") {
        context(1) {
            f1()
            f2(true)
        }
    }

    with("") {
        <!NO_CONTEXT_RECEIVER!>f1<!>(<!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>1<!>)<!>
        <!NO_CONTEXT_RECEIVER!>f2<!>(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_TYPE_MISMATCH!>true<!>)<!>
        <!INCONSISTENT_CONTEXT_ARGUMENTS!>f3<!>(1, true)
        <!INCONSISTENT_CONTEXT_ARGUMENTS!>f4<!>(1, "")
        <!NO_CONTEXT_RECEIVER!>f4<!>(<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>1)<!>
        <!INCONSISTENT_CONTEXT_ARGUMENTS!>f5<!>(1, "", true)
    }

    "".<!INCONSISTENT_CONTEXT_ARGUMENTS!>f3<!>(1, true)
}
