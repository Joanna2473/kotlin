/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.protected

public abstract class PublicAbstractClass protected constructor() {
    protected abstract val protectedVal: Int
    protected abstract var protectedVar: Any?

    protected abstract fun protectedFun()
}
