// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildList<!> {
        println(get(0))
    }
}
