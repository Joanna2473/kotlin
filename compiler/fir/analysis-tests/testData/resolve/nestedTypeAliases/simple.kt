// RUN_PIPELINE_TILL: BACKEND
// DUMP_IR

class Foo<T> {
    inner class Inner
    inner class Inner2<T2>
}

typealias InnerAlias<K> = Foo<K>.Inner
typealias InnerAlias2<K, K2> = Foo<K>.Inner2<K2>
typealias InnerAlias3<K, K2> = Foo<K2>.Inner2<K>

fun test() {
    val foo = Foo<String>()
    foo.InnerAlias()
    foo.InnerAlias2<String, Int>()
    foo.InnerAlias3<Int, String>()

    val aliasedInner = Foo<String>::InnerAlias
    aliasedInner(foo)
}