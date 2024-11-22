/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.sir.lightclasses.nodes.*

public class SirDeclarationFromKtSymbolProvider(
    private val ktModule: KaModule,
    private val sirSession: SirSession,
) : SirDeclarationProvider {

    override fun KaDeclarationSymbol.toSIR(): SirTranslationResult {
        return when (val ktSymbol = this@toSIR) {
            is KaNamedClassSymbol -> {
                createSirClassFromKtSymbol(
                    ktSymbol = ktSymbol,
                    ktModule = ktModule,
                    sirSession = sirSession,
                ).let(SirTranslationResult::Class)
            }
            is KaConstructorSymbol -> {
                SirInitFromKtSymbol(
                    ktSymbol = ktSymbol,
                    ktModule = ktModule,
                    sirSession = sirSession,
                ).let(SirTranslationResult::Init)
            }
            is KaNamedFunctionSymbol -> {
                SirFunctionFromKtSymbol(
                    ktSymbol = ktSymbol,
                    ktModule = ktModule,
                    sirSession = sirSession,
                ).let(SirTranslationResult::Function)
            }
            is KaVariableSymbol -> {
                ktSymbol.toSirVariable().let(SirTranslationResult::Variable)
            }
            is KaTypeAliasSymbol -> {
                SirTypealiasFromKtSymbol(
                    ktSymbol = ktSymbol,
                    ktModule = ktModule,
                    sirSession = sirSession,
                ).let(SirTranslationResult::TypeAlias)
            }
            else -> TODO("encountered unknown symbol type - $ktSymbol. Error system should be reworked KT-65980")
        }
    }

    private fun KaVariableSymbol.toSirVariable(): SirAbstractVariableFromKtSymbol = when (this) {
        is KaEnumEntrySymbol -> SirEnumCaseFromKtSymbol(
            ktSymbol = this,
            ktModule = ktModule,
            sirSession = sirSession,
        )
        else ->
            if (this is KaPropertySymbol
                && isStatic
                && name == StandardNames.ENUM_ENTRIES
            ) {
                SirEnumEntriesStaticPropertyFromKtSymbol(this, ktModule, sirSession)
            } else {
                SirVariableFromKtSymbol(
                    ktSymbol = this@toSirVariable,
                    ktModule = ktModule,
                    sirSession = sirSession,
                )
            }
    }
}
