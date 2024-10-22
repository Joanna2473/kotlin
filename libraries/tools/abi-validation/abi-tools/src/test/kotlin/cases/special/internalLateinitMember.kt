/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.special

public class ClassWithLateInitMembers internal constructor() {

    public lateinit var publicLateInitWithInternalSet: String
        internal set

    internal lateinit var internalLateInit: String

}