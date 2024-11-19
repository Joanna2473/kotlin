/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.validation.api

import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

public open class AbiValidationBaseTests : KGPBaseTest() {
    @Rule
    @JvmField
    internal val testProjectDir: TemporaryFolder = TemporaryFolder()

    internal val rootProjectDir: File get() = testProjectDir.root

    internal val rootProjectApiDump: File get() = rootProjectDir.resolve("$API_DIR/${rootProjectDir.name}.api")

    internal fun rootProjectAbiDump(project: String = rootProjectDir.name): File {
        return rootProjectDir.resolve("$API_DIR/$project.klib.api")
    }
}
