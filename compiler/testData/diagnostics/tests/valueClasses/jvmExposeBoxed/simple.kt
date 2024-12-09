// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@file:JvmExposeBoxed("foo")

@JvmExposeBoxed("foo")
class Foo
@JvmExposeBoxed("foo") constructor()
{
    @JvmExposeBoxed("foo")
    companion object {

    }
}

@JvmExposeBoxed("foo")
class Obj

@JvmInline
value class IC(val s: String)

@JvmExposeBoxed("foo")
val ic: IC = TODO()

@get:JvmExposeBoxed("foo")
val icic: IC = TODO()

@JvmExposeBoxed
fun foo(): IC = TODO()

@JvmExposeBoxed
fun bar(ic: IC) {}

@JvmExposeBoxed
fun foo(result: Result<Any>) {}

@JvmExposeBoxed
val ic1: IC = TODO()

@JvmExposeBoxed
var ic2: IC = TODO()

class C {
    @JvmExposeBoxed
    fun foo(): IC = TODO()

    @JvmExposeBoxed
    fun foo(result: Result<Any>) {}

    @JvmExposeBoxed
    val ic1: IC = TODO()

    @JvmExposeBoxed
    var ic2: IC = TODO()
}

@JvmExposeBoxed
fun Int.foo(i: Int) {}

@JvmExposeBoxed("foo")
@JvmName("foo")
fun bar1(ic: IC) {}

@JvmExposeBoxed("foo")
@JvmName("foo")
fun barIC(): IC = TODO()

@JvmExposeBoxed("...")
fun todo(ic: IC) {}

@JvmExposeBoxed
inline fun <reified T> inlineMe(ic: IC) {}

@JvmExposeBoxed("same")
fun same(): IC = TODO()

@JvmExposeBoxed
suspend fun suspendMe(ic: IC) {}
