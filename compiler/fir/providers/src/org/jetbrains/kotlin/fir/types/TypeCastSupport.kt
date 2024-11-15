/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

interface TypeCastSupport : FirSessionComponent {
    /**
     * Remember that we are trying to cast something of type `supertype` to `subtype`.

     * Since at runtime we can only check the class (type constructor), the rest of the subtype should be known statically, from supertype.
     * This method reconstructs all static information that can be obtained from supertype.

     * Example 1:
     * supertype = Collection
     * subtype = List<...>
     * result = List, all arguments are inferred

     * Example 2:
     * supertype = Any
     * subtype = List<...>
     * result = List<*>, some arguments were not inferred, replaced with '*'
     */
    fun createStaticallyKnownSubtype(
        supertype: ConeKotlinType,
        subTypeClassSymbol: FirRegularClassSymbol,
        isSubTypeMarkedNullable: Boolean,
        attributes: ConeAttributes,
        session: FirSession,
    ): ConeKotlinType
}

val FirSession.typeCastSupport: TypeCastSupport by FirSession.sessionComponentAccessor()
