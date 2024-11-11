/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer

@Suppress("unused")
object PhaseConfigConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.config", "PhaseConfigConfigurationKeys") {
    val DISABLE_PHASES by key<List<String>>("List of phases to disable")
    val VERBOSE_PHASES by key<List<String>>("List of phases with verbose output")
    val PHASES_TO_DUMP_BEFORE by key<List<String>>("Dump the backend's state before these phases")
    val PHASES_TO_DUMP_AFTER by key<List<String>>("Dump the backend's state after these phases")
    val PHASES_TO_DUMP by key<List<String>>("Dump the backend's state both before and after these phases")
    val PHASE_DUMP_DIRECTORY by key<String>("Dump the backend state into this directory", allowNull = true)
    val PHASE_DUMP_ONLY_FQ_NAME by key<String>("Dump the declaration with the given FqName", allowNull = true)

    val PHASES_TO_VALIDATE_BEFORE by key<List<String>>("Validate the backend's state before these phases")
    val PHASES_TO_VALIDATE_AFTER by key<List<String>>("Validate the backend's state after these phases")
    val PHASES_TO_VALIDATE by key<List<String>>("Validate the backend's state both before and after these phases")

    val NEED_PROFILE_PHASES by key<Boolean>("Profile backend phases")
    val CHECK_PHASE_CONDITIONS by key<Boolean>("Check pre- and postconditions of IR lowering phases")
    val CHECK_STICKY_PHASE_CONDITIONS by key<Boolean>("Run sticky condition checks on subsequent phases. Implicitly enables '-Xcheck-phase-conditions'")
}
