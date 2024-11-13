/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.protected

public class PublicFinalClass protected constructor() {
    protected val protectedVal = 1
    protected var protectedVar = 2

    protected fun protectedFun() = protectedVal
}
