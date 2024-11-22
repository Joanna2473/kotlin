// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
fun test(f: <!UNSUPPORTED_FEATURE!>context(String, Int)<!> Boolean.() -> Unit) {
    "".f(1, true)
}