// FIR_IDENTICAL
// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

interface Tr
interface G<T>

fun test(tr: Tr) {
    val v = tr as G?
    // If v is not nullable, there will be a warning on this line:
    checkSubtype<G<*>>(v!!)
}
