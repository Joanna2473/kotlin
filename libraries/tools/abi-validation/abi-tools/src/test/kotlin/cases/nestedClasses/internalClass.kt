/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.nestedClasses

internal class InternalClass {
    public object ObjPublic
    internal object ObjInternal
    protected object ObjProtected
    private object ObjPrivate

    public class NestedPublic
    internal class NestedInternal
    protected class NestedProtected
    private class NestedPrivate

    public interface NestedPublicInterface
    internal interface NestedInternalInterface
    protected interface NestedProtectedInterface
    private interface NestedPrivateInterface

    public inner class InnerPublic
    internal inner class InnerInternal
    protected inner class InnerProtected
    private inner class InnerPrivate
}

