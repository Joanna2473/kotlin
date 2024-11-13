/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.nestedClasses

internal object InternalObject {

    public object ObjPublic
    internal object ObjInternal
    private object ObjPrivate

    public class NestedPublic
    internal class NestedInternal
    private class NestedPrivate

    public interface NestedPublicInterface
    internal interface NestedInternalInterface
    private interface NestedPrivateInterface
}

