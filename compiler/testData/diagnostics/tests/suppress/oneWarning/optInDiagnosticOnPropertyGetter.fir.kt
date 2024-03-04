// DIAGNOSTICS: -ERROR_SUPPRESSION
// WITH_STDLIB
// ISSUE: KT-66258

@RequiresOptIn
annotation class Ann

class A(
    <!OPT_IN_MARKER_ON_WRONG_TARGET!>@get:Ann<!>
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    val s: String
)

class B {
    @get:Ann
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    val s: String = ""
}
