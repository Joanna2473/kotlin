// RUN_PIPELINE_TILL: BACKEND
fun foo() {
    val base = object {
        fun bar() = object {
            fun buz() = foobar
        }
        val foobar = ""
    }
}