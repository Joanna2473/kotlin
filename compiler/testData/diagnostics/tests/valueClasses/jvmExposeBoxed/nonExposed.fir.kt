// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@file:JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME, JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)

@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME, JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)
class Foo
<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME, JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)<!> constructor()
{
    @JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME, JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)
    companion object {

}
}

@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME, JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)
class Obj

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)
fun shouldBeWarning(u: UInt) {}

@JvmExposeBoxed(expose = false)
fun noWarning(u: UInt) {}

@JvmInline
value class IC(val s: String)

@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME, JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)
val ic: IC = TODO()

@get:JvmExposeBoxed(<!JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)
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

<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed(expose = false)<!>
fun Int.foo(i: Int) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)
@JvmName("foo")
fun bar1(ic: IC) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME, JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"foo"<!>, false)
@JvmName("foo")
fun barIC(): IC = TODO()

@JvmExposeBoxed(<!ILLEGAL_JVM_NAME, JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"..."<!>, false)
fun todo(ic: IC) {}

<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed(expose = false)<!>
inline fun <reified T> inlineMe(ic: IC) {}

<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME!>@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_REDUNDANT_NAME!>"same"<!>, expose = false)<!>
fun same(): IC = TODO()

@JvmExposeBoxed(expose = false)
suspend fun suspendMe(ic: IC) {}
