/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.protected

public class PublicFinalClass protected constructor() {
    protected val protectedVal = 1
    protected var protectedVar = 2

    protected fun protectedFun() = protectedVal
}
