/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.special

public class ClassWithLateInitMembers internal constructor() {

    public lateinit var publicLateInitWithInternalSet: String
        internal set

    internal lateinit var internalLateInit: String

}