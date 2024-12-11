/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.analysis.api.documentation.check

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import java.io.File
import kotlin.system.measureNanoTime

class AnalysisApiDocumentationTest : AbstractRawFirBuilderTestCase() {
    private fun testDocumentationByFile(file: KtFile) {
        val allowedDeclarations = file.getAllowedDeclarations()
        allowedDeclarations.forEach {
            assertTrue(
                "${it.name} from ${it.containingKtFile.virtualFilePath}:${it.containingClassOrObject?.name ?: ""} is not documented",
                it.docComment != null
            )
        }
    }

    private val nonPublicAnnotations = listOf(
        "@KaImplementationDetail",
        "@KaNonPublicApi",
        "@KaIdeApi",
        "@KaExperimentalApi",
        "@KaPlatformInterface"
    )

    private val ignoredPropertyNames = listOf(
        "symbol",
        "name",
        "libraryName",
        "typeArguments",
        "token"
    )

    private val ignoredFunctionNames = listOf(
        "create",
        "getInstance",
        "getModule"
    )

    private fun KtFile.getAllowedDeclarations(): List<KtDeclaration> = this.declarations.flatMap { it.getAllowedNestedDeclarations() }

    private fun KtDeclaration.getAllowedNestedDeclarations(): List<KtDeclaration> {
        if (!this.isAllowed()) return emptyList()

        return buildList {
            add(this@getAllowedNestedDeclarations)
            if (this@getAllowedNestedDeclarations is KtDeclarationContainer) {
                addAll(this@getAllowedNestedDeclarations.declarations.flatMap { it.getAllowedNestedDeclarations() })
            }
        }
    }

    private fun KtDeclaration.isAllowed(): Boolean {
        if ((this as? KtClass)?.isAnnotation() == true) return false

        if (!this.isPublic || this.annotationEntries.any { it.text in nonPublicAnnotations })
            return false

        if (this is KtProperty) {
            if (this.name in ignoredPropertyNames || this.hasModifier(KtTokens.OVERRIDE_KEYWORD))
                return false
        }

        if (this is KtNamedFunction) {
            if (this.name in ignoredFunctionNames || this.hasModifier(KtTokens.OVERRIDE_KEYWORD))
                return false
        }

        return true
    }


    fun getKtFile(text: String, path: String): KtFile {
        return createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(path)), text) as KtFile
    }

    fun testAnalysisApiIsDocumented() {
        val path = System.getProperty("user.dir") + "/analysis/analysis-api/src"
        val root = File(path)
        var counter = 0
        var time = 0L

        println("BASE PATH: $path")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.extension != "kt") continue

            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            time += measureNanoTime {
                try {
                    val file = getKtFile(text, file.path)
                    testDocumentationByFile(file)
                } catch (e: Exception) {
                    throw IllegalStateException(file.path, e)
                }
            }

            counter++
        }
        println("SUCCESS!")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")
    }
}
