/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.interfaces

public interface BaseWithImpl {
    fun foo() = 42
}

public interface DerivedWithImpl : BaseWithImpl {
    override fun foo(): Int {
        return super.foo() + 1
    }
}

public interface DerivedWithoutImpl : BaseWithImpl

