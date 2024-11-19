// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NestedTypeAliases

class A<T>

class B<T> {
    typealias NestedTA = A<<!UNRESOLVED_REFERENCE!>T<!>> // T should be UNRESOLVED
    inner typealias InnerTA = A<T> // OK
}

