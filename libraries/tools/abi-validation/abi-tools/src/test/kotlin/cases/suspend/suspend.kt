/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.suspend

public interface I {
    suspend fun openFunction(): Int = 42
}

public interface II : I {
    override suspend fun openFunction(): Int {
        return super.openFunction() + 1
    }
}

public open class C : II {
    override suspend fun openFunction(): Int {
        return super.openFunction() + 2
    }
}
