// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NestedTypeAliases

class A<T>

class C<T> {
    inner typealias TA = A<T>
}
