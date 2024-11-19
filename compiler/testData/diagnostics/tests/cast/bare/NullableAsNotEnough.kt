// FIR_IDENTICAL
// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface Tr
interface G<T>

fun test(tr: Tr?) {
    val v = tr as <!NO_TYPE_ARGUMENTS_ON_RHS!>G<!>
    checkSubtype<G<*>>(v)
}