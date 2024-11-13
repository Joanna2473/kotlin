/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.special

@JvmName("internalFun")
internal fun internalRenamedFun() {}

internal var internalVar: Int = 1
    @JvmName("internalVarGetter")
    get
    @JvmName("internalVarSetter")
    set

@JvmName("publicFun")
public fun publicRenamedFun() {}

public var publicVar: Int = 1
    @JvmName("publicVarGetter")
    get
    @JvmName("publicVarSetter")
    set



