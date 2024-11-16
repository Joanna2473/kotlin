// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71732

class Test

fun testBuilder(id: String = "", lambda: Test.() -> Unit) = Test()

fun test() {
    contract(<!OTHER_ERROR_WITH_REASON("Cannot calculate return type during full-body resolution (local class/object?): public final fun testBuilder(id: R|kotlin/String| = String(), lambda: R|Test.() -> kotlin/Unit|): <implicit> {    ^testBuilder Test#()}")!>testBuilder {}<!>)
}

fun contract(test: Test) {}
