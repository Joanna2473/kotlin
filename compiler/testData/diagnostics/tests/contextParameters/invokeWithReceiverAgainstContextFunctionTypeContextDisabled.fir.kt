// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ContextParameters
fun test(f: <!UNSUPPORTED_FEATURE!><!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String, Int)<!> Boolean.() -> Unit) {
    "".<!UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL!>f<!>(1, true)
}