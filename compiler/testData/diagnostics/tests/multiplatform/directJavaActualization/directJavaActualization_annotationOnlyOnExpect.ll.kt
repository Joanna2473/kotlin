// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

annotation class Annot

@Annot
expect class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.jvm.KotlinActual
public class Foo {}
