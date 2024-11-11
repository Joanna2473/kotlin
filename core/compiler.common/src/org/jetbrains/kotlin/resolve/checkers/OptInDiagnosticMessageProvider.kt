/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers


abstract class OptInDiagnosticMessageProvider {
    abstract fun buildDiagnosticMessage(markerName: String, verb: String = "", customMessage: String? = null): String
}

object OptInUsagesDiagnosticMessageProvider : OptInDiagnosticMessageProvider() {
    override fun buildDiagnosticMessage(markerName: String, verb: String, customMessage: String?): String {
        val prefix = "This declaration requires opt-in"
        val prefixWithCustomMessage = buildPrefixWithCustomMessage(prefix, customMessage)
        return OptInNames.buildDefaultDiagnosticMessage("$prefixWithCustomMessage Its usage $verb be marked", markerName)
    }
}

object OptInUsagesInFutureDiagnosticMessageProvider : OptInDiagnosticMessageProvider() {
    override fun buildDiagnosticMessage(markerName: String, verb: String, customMessage: String?): String =
        OptInNames.buildDefaultDiagnosticMessage(
            "This declaration is experimental due to signature types and its usage $verb be marked (will become an error in future releases)",
            markerName
        )

    fun buildCustomDiagnosticMessage(message: String): String = message
}

object OptInInheritanceDiagnosticMessageProvider : OptInDiagnosticMessageProvider() {
    override fun buildDiagnosticMessage(markerName: String, verb: String, customMessage: String?): String {
        val prefix = "This class or interface requires opt-in to be implemented"
        val prefixWithCustomMessage = buildPrefixWithCustomMessage(prefix, customMessage)
        return OptInNames.buildDefaultDiagnosticMessage(
            "$prefixWithCustomMessage Its usage $verb be marked",
            markerName,
            isSubclassOptInApplicable = true
        )
    }
}

object ExperimentalUnsignedLiteralsDiagnosticMessageProvider : OptInDiagnosticMessageProvider() {
    override fun buildDiagnosticMessage(markerName: String, verb: String, customMessage: String?): String =
        OptInNames.buildDefaultDiagnosticMessage("Unsigned literals are experimental and their usages $verb be marked", markerName)
}

fun String.endsWithSentenceTerminator(): Boolean = this.trim().matches(Regex(".*[.!?]$"))

fun buildPrefixWithCustomMessage(prefix: String, customMessage: String?): String {
    val customMessagePart = if (customMessage.isNullOrBlank()) prefix else "$prefix: $customMessage".trim()
    return if (customMessagePart.endsWithSentenceTerminator()) customMessagePart else "$customMessagePart."
}