// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:JvmExposeBoxed
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class StringWrapper(val s: String)

@JvmExposeBoxed(expose = false)
class Foo {
    fun mangled(): StringWrapper = StringWrapper("FAIL")

    @JvmExposeBoxed
    fun ok(): StringWrapper = StringWrapper("OK")
}

// FILE: Main.java
public class Main {
    public String test() {
        return new Foo().ok().getS();
    }
}

// FILE: Box.kt
fun box(): String {
    return Main().test()
}