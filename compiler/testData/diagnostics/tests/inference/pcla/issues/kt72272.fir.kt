// WITH_STDLIB
// ISSUE: KT-72272

val obj: String? = "hi"

fun foo() = buildList {
    try {
        var i = 0
        do {
            i++
            obj?.let {
                networkCall(it)
            } ?: run {
                <!ARGUMENT_TYPE_MISMATCH, TYPE_MISMATCH!>log()<!> // this is the error line
            }
        } while(i < 3)
    } catch(ex: Exception) {
        log()
    }
    add(false)
}

fun networkCall(bar: String?) : String {
    return "yes"
}

fun log() {
    // no op
}
