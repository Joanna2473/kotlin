// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)
@file:JvmExposeBoxed

@JvmInline
value class StringWrapper(val s: String)

// Just to force the compiler to generate a classfile for the file
fun foo() {}

// FILE: Main.java
public class Main {
    public String test() {
        return new StringWrapper("OK").getS();
    }
}

// FILE: Box.kt
fun box(): String {
    return Main().test()
}