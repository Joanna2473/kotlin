// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>@file:JvmExposeBoxed("foo")<!>

<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>@JvmExposeBoxed("foo")<!>
class Foo
<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME, USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed("foo")<!> constructor()
{
    <!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>@JvmExposeBoxed("foo")<!>
    companion object {

    }
}

<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>@JvmExposeBoxed("foo")<!>
class Obj

@JvmInline
value class IC(val s: String)

<!INAPPLICABLE_JVM_EXPOSE_BOXED_WITH_NAME!>@JvmExposeBoxed("foo")<!>
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
fun bar(ic: IC) {}

<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME!>@JvmExposeBoxed("foo")<!>
@JvmName("foo")
fun barIC(): IC = TODO()

<!ILLEGAL_JVM_NAME!>@JvmExposeBoxed("...")<!>
fun todo(ic: IC) {}

<!USELESS_JVM_EXPOSE_BOXED!>@JvmExposeBoxed<!>
inline fun <reified T> inlineMe(ic: IC) {}

@JvmExposeBoxed("same")
fun same(): IC = TODO()