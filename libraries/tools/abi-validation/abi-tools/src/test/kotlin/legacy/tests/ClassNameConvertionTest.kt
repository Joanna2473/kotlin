/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.abi.tools.legacy

import org.jetbrains.kotlin.abi.tools.klib.toAbiQualifiedName
import org.jetbrains.kotlin.library.abi.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClassNameConvertionTest {
    @Test
    fun testConvertBinaryName() {
        assertNull("".toAbiQualifiedName())
        assertNull("   ".toAbiQualifiedName())
        assertNull("a/b/c/d".toAbiQualifiedName())
        assertNull("a.b.c/d.e".toAbiQualifiedName())

        checkNames("Hello", AbiQualifiedName("", "Hello"))
        checkNames("a.b.c", AbiQualifiedName("a.b", "c"))
        checkNames("a\$b\$c", AbiQualifiedName("", "a.b.c"))
        checkNames("p.a\$b\$c", AbiQualifiedName("p", "a.b.c"))
        checkNames("org.example.Outer\$Inner\$\$serializer",
            AbiQualifiedName("org.example", "Outer.Inner.\$serializer"))
        checkNames("org.example.Outer\$Inner\$\$\$serializer",
            AbiQualifiedName("org.example", "Outer.Inner.\$\$serializer"))
        checkNames("a.b.e.s.c.MapStream\$Stream\$",
            AbiQualifiedName("a.b.e.s.c", "MapStream.Stream\$"))
    }

    private fun checkNames(binaryClassName: String, qualifiedName: AbiQualifiedName) {
        val converted = binaryClassName.toAbiQualifiedName()!!
        assertEquals(qualifiedName.packageName, converted.packageName)
        assertEquals(qualifiedName.relativeName, converted.relativeName)
    }
}

private fun AbiQualifiedName(packageName: String, className: String) =
    AbiQualifiedName(AbiCompoundName(packageName), AbiCompoundName(className))
