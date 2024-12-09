// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@file:JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)

@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)
class Foo
<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)<!> constructor()
{
    @JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)
    companion object {

    }
}

@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)
class Obj

@JvmInline
value class IC(val s: String)

@JvmExposeBoxed(<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>"foo"<!>)
val ic: IC = TODO()

@get:JvmExposeBoxed("foo")
val icic: IC = TODO()

<!JVM_EXPOSE_BOXED_REQUIRES_NAME!>@JvmExposeBoxed<!>
fun foo(): IC = TODO()

@JvmExposeBoxed
fun bar(ic: IC) {}

@JvmExposeBoxed
fun foo(result: Result<Any>) {}

<!JVM_EXPOSE_BOXED_REQUIRES_NAMED_GETTER!>@JvmExposeBoxed<!>
val ic1: IC = TODO()

<!JVM_EXPOSE_BOXED_REQUIRES_NAMED_GETTER!>@JvmExposeBoxed<!>
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

<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!>
fun Int.foo(i: Int) {}

@JvmExposeBoxed("foo")
@JvmName("foo")
fun bar1(ic: IC) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME!>"foo"<!>)
@JvmName("foo")
fun barIC(): IC = TODO()

@JvmExposeBoxed(<!ILLEGAL_JVM_NAME!>"..."<!>)
fun todo(ic: IC) {}

<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!>
inline fun <reified T> inlineMe(ic: IC) {}

<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME!>@JvmExposeBoxed("same")<!>
fun same(): IC = TODO()

<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SUSPEND!>@JvmExposeBoxed<!>
suspend fun suspendMe(ic: IC) {}
