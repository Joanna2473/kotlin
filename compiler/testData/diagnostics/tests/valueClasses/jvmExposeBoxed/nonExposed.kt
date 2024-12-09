// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@file:JvmExposeBoxed("foo", false)

@JvmExposeBoxed("foo", false)
class Foo
@JvmExposeBoxed("foo", false) constructor()
{
    @JvmExposeBoxed("foo", false)
    companion object {

}
}

@JvmExposeBoxed("foo", false)
class Obj

@JvmExposeBoxed("foo", false)
fun shouldBeWarning(u: UInt) {}

@JvmExposeBoxed(expose = false)
fun noWarning(u: UInt) {}

@JvmInline
value class IC(val s: String)

@JvmExposeBoxed("foo", false)
val ic: IC = TODO()

@get:JvmExposeBoxed("foo", false)
val icic: IC = TODO()

@JvmExposeBoxed(expose = false)
fun foo(): IC = TODO()

@JvmExposeBoxed(expose = false)
fun bar(ic: IC) {}

@JvmExposeBoxed
fun foo(result: Result<Any>) {}

@JvmExposeBoxed(expose = false)
val ic1: IC = TODO()

@JvmExposeBoxed(expose = false)
var ic2: IC = TODO()

class C {
    @JvmExposeBoxed(expose = false)
    fun foo(): IC = TODO()

    @JvmExposeBoxed(expose = false)
    fun foo(result: Result<Any>) {}

    @JvmExposeBoxed(expose = false)
    val ic1: IC = TODO()

    @JvmExposeBoxed(expose = false)
    var ic2: IC = TODO()
}

@JvmExposeBoxed(expose = false)
fun Int.foo(i: Int) {}

@JvmExposeBoxed("foo", false)
@JvmName("foo")
fun bar1(ic: IC) {}

@JvmExposeBoxed("foo", false)
@JvmName("foo")
fun barIC(): IC = TODO()

@JvmExposeBoxed("...", false)
fun todo(ic: IC) {}

@JvmExposeBoxed(expose = false)
inline fun <reified T> inlineMe(ic: IC) {}

@JvmExposeBoxed("same", expose = false)
fun same(): IC = TODO()

@JvmExposeBoxed(expose = false)
suspend fun suspendMe(ic: IC) {}
