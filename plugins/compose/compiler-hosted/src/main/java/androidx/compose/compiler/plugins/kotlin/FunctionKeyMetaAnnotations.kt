/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

class FunctionKeyMetaAnnotations(
    private val locations: List<Location> = emptyList(),
) {

    fun isEnabled(location: Location): Boolean = locations.contains(location)

    enum class Location {
        Function, Class
    }
}