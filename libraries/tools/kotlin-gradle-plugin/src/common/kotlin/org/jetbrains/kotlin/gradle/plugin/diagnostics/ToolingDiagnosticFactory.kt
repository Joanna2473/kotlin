/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@InternalKotlinGradlePluginApi // used in integration tests
abstract class ToolingDiagnosticFactory(
    private val predefinedSeverity: ToolingDiagnostic.Severity? = null,
    customId: String? = null,
) {
    open val id: String = customId ?: this::class.simpleName!!

    protected fun build(
        name: String,
        message: String,
        solutions: List<String> = emptyList(),
        documentation: ToolingDiagnostic.Documentation? = null,
        severity: ToolingDiagnostic.Severity? = null,
        throwable: Throwable? = null,
    ): ToolingDiagnostic {
        if (severity == null && predefinedSeverity == null) {
            error(
                "Can't determine severity. " +
                        "Either provide it in constructor of ToolingDiagnosticFactory, or in the 'build'-function invocation"
            )
        }
        if (severity != null && predefinedSeverity != null) {
            error(
                "Please provide severity either in ToolingDiagnosticFactory constructor, or as the 'build'-function parameter," +
                        " but not both at once"
            )
        }

        return ToolingDiagnostic(
            identifier = ToolingDiagnostic.ID(id, name),
            message = message,
            severity = severity ?: predefinedSeverity!!,
            solutions = solutions,
            documentation = documentation,
            throwable = throwable
        )
    }

    internal fun build(
        severity: ToolingDiagnostic.Severity? = null,
        throwable: Throwable? = null,
        builder: ToolingDiagnosticBuilder.() -> Unit,
    ) = ToolingDiagnosticBuilder().apply(builder).let {
        build(it.name, it.message, it.solutions, it.documentation, severity, throwable)
    }

    protected fun String.onlyIf(condition: Boolean) = if (condition) this else ""
}

@Suppress("unused")
internal class ToolingDiagnosticBuilder {

    val name: String get() = _name ?: error("Name is not provided")
    val message: String get() = _message ?: error("Message is not provided")
    val solutions: List<String> get() = _solutions.toList()
    val documentation: ToolingDiagnostic.Documentation? get() = _documentation

    private var _name: String? = null
    private var _message: String? = null
    private var _solutions: MutableList<String> = mutableListOf()
    private var _documentation: ToolingDiagnostic.Documentation? = null

    fun name(string: () -> String) {
        _name = string()
    }

    fun message(string: () -> String) {
        _message = string()
    }

    private fun checkSolutionIsSingleLine(text: String) {
        check(text.lines().size == 1) {
            """
                Solution should not be multi-line:
                $text
            """.trimIndent()
        }
    }

    fun solution(singleString: () -> String) {
        singleString().takeIf { it.isNotBlank() }?.let {
            checkSolutionIsSingleLine(it)
            _solutions.add(it)
        }
    }

    fun solutions(stringList: () -> List<String>) {
        stringList().filter { it.isNotBlank() }.let { strings ->
            strings.forEach(::checkSolutionIsSingleLine)
            _solutions.addAll(strings)
        }
    }

    fun documentation(url: String, urlBuilder: (String) -> String = { "See $url for more details." }) {
        _documentation = ToolingDiagnostic.Documentation(url, urlBuilder(url))
    }
}