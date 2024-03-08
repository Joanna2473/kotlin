// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-AAPCS-LABEL: define i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-DEFAULTABI-LABEL: define zeroext i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK-WINDOWSX64-LABEL: define zeroext i1 @"kfun:C#equals(kotlin.Any?){}kotlin.Boolean"(ptr %0, ptr %1)
// CHECK: call ptr @"kfun:#<C-unbox>(kotlin.Any?){}C?"
value class C(val x: Any)
// Note: <C-unbox> is also called from bridges for equals, hashCode and toString.

fun box() =
    if (C(42) == C(13)) "FAIL" else "OK"
