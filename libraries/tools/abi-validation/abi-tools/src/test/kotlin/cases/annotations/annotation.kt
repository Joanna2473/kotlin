/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.annotations

annotation class Foo(val i: Int)

private class Bar {
    val foo: Foo = Foo(1) // Same module
    val e = Volatile() // Cross-module

    fun bar() {
        foo()
    }
}
