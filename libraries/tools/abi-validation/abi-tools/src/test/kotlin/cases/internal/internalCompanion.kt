/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.internal

// Internal companion is not part of public API
// neither should be outer static final companion field

// Named that way so the test ensures that PublicClassInternalCompanionWithNameShadowing does properly differentiate
// 'Companion' static final field and hand-written field with the same type
class Companion {
    internal companion object
}

class PublicClassNamedInternalCompanion {
    internal companion object Foo
}

class PublicClassInternalCompanionWithNameShadowing {
    internal companion object Companion {
        @JvmStatic
        public val Companion: cases.internal.Companion = Companion()
    }
}
