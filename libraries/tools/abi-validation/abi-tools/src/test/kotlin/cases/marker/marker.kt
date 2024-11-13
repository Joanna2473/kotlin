/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.marker

@Target(AnnotationTarget.FIELD)
annotation class HiddenField

@Target(AnnotationTarget.PROPERTY)
annotation class HiddenProperty

annotation class HiddenMethod

public class Foo {
    // HiddenField will be on the field
    @HiddenField
    var bar1 = 42

    // HiddenField will be on a synthetic `$annotations()` method
    @HiddenProperty
    var bar2 = 42

    @HiddenMethod
    fun hiddenMethod(bar: Int = 42) {
    }
}

