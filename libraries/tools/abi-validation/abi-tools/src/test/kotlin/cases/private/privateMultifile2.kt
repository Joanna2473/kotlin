/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("MultifileKt")
@file:JvmMultifileClass
package cases.private


// const
private const val privateConst: Int = 4

// fun
@Suppress("UNUSED_PARAMETER")
private fun privateFun(x: Any) {}


private class PrivateClassInMultifile {
    internal fun accessUsage() {
        privateFun(privateConst)
    }

}
