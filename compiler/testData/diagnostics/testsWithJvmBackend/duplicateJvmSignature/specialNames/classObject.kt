// IGNORE_FIR_DIAGNOSTICS
// FIR_IDENTICAL

class C {
    companion <!REDECLARATION!>object<!> {}

    val <!REDECLARATION!>Companion<!> = C
}
