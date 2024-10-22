// ISSUE: KT-72238

interface Box<F> {
    fun add(f: F)
}

fun <E> myBuilder(x: Box<E>.() -> Unit) {}

fun <T, R> T.myRun(block: T.() -> R): R = TODO()

fun String.foo(result: String?) {
    myBuilder {
        result ?: myRun { <!ARGUMENT_TYPE_MISMATCH!>Unit<!> }
        add("a")
    }
}
