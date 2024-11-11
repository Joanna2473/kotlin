/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.config

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */


object PhaseConfigConfigurationKeys {
    @JvmField
    val DISABLE_PHASES = CompilerConfigurationKey.create<List<String>>("List of phases to disable")

    @JvmField
    val VERBOSE_PHASES = CompilerConfigurationKey.create<List<String>>("List of phases with verbose output")

    @JvmField
    val PHASES_TO_DUMP_BEFORE = CompilerConfigurationKey.create<List<String>>("Dump the backend's state before these phases")

    @JvmField
    val PHASES_TO_DUMP_AFTER = CompilerConfigurationKey.create<List<String>>("Dump the backend's state after these phases")

    @JvmField
    val PHASES_TO_DUMP = CompilerConfigurationKey.create<List<String>>("Dump the backend's state both before and after these phases")

    @JvmField
    val PHASE_DUMP_DIRECTORY = CompilerConfigurationKey.create<String>("Dump the backend state into this directory")

    @JvmField
    val PHASE_DUMP_ONLY_FQ_NAME = CompilerConfigurationKey.create<String>("Dump the declaration with the given FqName")

    @JvmField
    val PHASES_TO_VALIDATE_BEFORE = CompilerConfigurationKey.create<List<String>>("Validate the backend's state before these phases")

    @JvmField
    val PHASES_TO_VALIDATE_AFTER = CompilerConfigurationKey.create<List<String>>("Validate the backend's state after these phases")

    @JvmField
    val PHASES_TO_VALIDATE = CompilerConfigurationKey.create<List<String>>("Validate the backend's state both before and after these phases")

    @JvmField
    val NEED_PROFILE_PHASES = CompilerConfigurationKey.create<Boolean>("Profile backend phases")

    @JvmField
    val CHECK_PHASE_CONDITIONS = CompilerConfigurationKey.create<Boolean>("Check pre- and postconditions of IR lowering phases")

    @JvmField
    val CHECK_STICKY_PHASE_CONDITIONS = CompilerConfigurationKey.create<Boolean>("Run sticky condition checks on subsequent phases. Implicitly enables '-Xcheck-phase-conditions'")

}

var CompilerConfiguration.disablePhases: MutableList<String>
    get() = getList(PhaseConfigConfigurationKeys.DISABLE_PHASES)
    set(value) { put(PhaseConfigConfigurationKeys.DISABLE_PHASES, value) }

var CompilerConfiguration.verbosePhases: MutableList<String>
    get() = getList(PhaseConfigConfigurationKeys.VERBOSE_PHASES)
    set(value) { put(PhaseConfigConfigurationKeys.VERBOSE_PHASES, value) }

var CompilerConfiguration.phasesToDumpBefore: MutableList<String>
    get() = getList(PhaseConfigConfigurationKeys.PHASES_TO_DUMP_BEFORE)
    set(value) { put(PhaseConfigConfigurationKeys.PHASES_TO_DUMP_BEFORE, value) }

var CompilerConfiguration.phasesToDumpAfter: MutableList<String>
    get() = getList(PhaseConfigConfigurationKeys.PHASES_TO_DUMP_AFTER)
    set(value) { put(PhaseConfigConfigurationKeys.PHASES_TO_DUMP_AFTER, value) }

var CompilerConfiguration.phasesToDump: MutableList<String>
    get() = getList(PhaseConfigConfigurationKeys.PHASES_TO_DUMP)
    set(value) { put(PhaseConfigConfigurationKeys.PHASES_TO_DUMP, value) }

var CompilerConfiguration.phaseDumpDirectory: String?
    get() = get(PhaseConfigConfigurationKeys.PHASE_DUMP_DIRECTORY)
    set(value) { putIfNotNull(PhaseConfigConfigurationKeys.PHASE_DUMP_DIRECTORY, value) }

var CompilerConfiguration.phaseDumpOnlyFqName: String?
    get() = get(PhaseConfigConfigurationKeys.PHASE_DUMP_ONLY_FQ_NAME)
    set(value) { putIfNotNull(PhaseConfigConfigurationKeys.PHASE_DUMP_ONLY_FQ_NAME, value) }

var CompilerConfiguration.phasesToValidateBefore: MutableList<String>
    get() = getList(PhaseConfigConfigurationKeys.PHASES_TO_VALIDATE_BEFORE)
    set(value) { put(PhaseConfigConfigurationKeys.PHASES_TO_VALIDATE_BEFORE, value) }

var CompilerConfiguration.phasesToValidateAfter: MutableList<String>
    get() = getList(PhaseConfigConfigurationKeys.PHASES_TO_VALIDATE_AFTER)
    set(value) { put(PhaseConfigConfigurationKeys.PHASES_TO_VALIDATE_AFTER, value) }

var CompilerConfiguration.phasesToValidate: MutableList<String>
    get() = getList(PhaseConfigConfigurationKeys.PHASES_TO_VALIDATE)
    set(value) { put(PhaseConfigConfigurationKeys.PHASES_TO_VALIDATE, value) }

var CompilerConfiguration.needProfilePhases: Boolean
    get() = getBoolean(PhaseConfigConfigurationKeys.NEED_PROFILE_PHASES)
    set(value) { put(PhaseConfigConfigurationKeys.NEED_PROFILE_PHASES, value) }

var CompilerConfiguration.checkPhaseConditions: Boolean
    get() = getBoolean(PhaseConfigConfigurationKeys.CHECK_PHASE_CONDITIONS)
    set(value) { put(PhaseConfigConfigurationKeys.CHECK_PHASE_CONDITIONS, value) }

var CompilerConfiguration.checkStickyPhaseConditions: Boolean
    get() = getBoolean(PhaseConfigConfigurationKeys.CHECK_STICKY_PHASE_CONDITIONS)
    set(value) { put(PhaseConfigConfigurationKeys.CHECK_STICKY_PHASE_CONDITIONS, value) }

