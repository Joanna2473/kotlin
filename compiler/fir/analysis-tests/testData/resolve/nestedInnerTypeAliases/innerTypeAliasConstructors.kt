// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NestedTypeAliases

class Pair<X, Y>(val x: X, val y: Y)
class Triple<X, Y, Z>(val x: X, val y: Y, val z: Z)
class D<T>

class C<T> {
    inner typealias P = Pair<T, T>

    inner typealias P1<X> = Pair<X, T>
    inner typealias P2<Y> = Pair<T, Y>

    inner typealias TripleTA<X, Y> = Triple<T, X, Y>

    inner typealias P3<Z> = P1<Z>

    inner typealias TA = D<T>
    inner typealias TA2 = C<T>

    inner class Inner

    inner typealias InnerTA = Inner

    inner class InnerWithTypeParam<K>

    inner typealias InnerTA2<K> = InnerWithTypeParam<K>

    fun test() {
        //val p_0: C<Int>.P<String> = P<String>(0, 0) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
        //val p_1: C<Int>.P = P(0, 0) // ARGUMENT_TYPE_MISMATCH

        //val inner_0 = Inner() // OK
        val innerTA_0 = InnerTA() // OK
        //val innerTA2_1 = InnerTA2<String>() // OK
    }
}

fun test() {
    val c = C<Int>()

    /*val p_0: C<Int>.P<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!> = c.P<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>(0, 0) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
    val p_1: C<Int>.P = c.P(0, 0) // OK

    val p1_0: C<Int>.P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!> = c.P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!>("str1", 1) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
    val p1_1: C<Int>.P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int, Char><!> = c.P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int, Char><!>("str1", 1) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
    val p1_2: C<Int>.P1<String> = c.P1<String>("str1", 1) // OK
    val p1_3: C<Int>.P1<String> = c.P1<String>(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>"str1"<!>) // ARGUMENT_TYPE_MISMATCH
    val p1_4: C<Int>.P1<String> = c.P1("str1", 1) // OK

    val p2_0: C<Int>.P2<String> = c.P2<String>(<!ARGUMENT_TYPE_MISMATCH!>"str2"<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>) // ARGUMENT_TYPE_MISMATCH
    val p2_1: C<Int>.P2<String> = c.P2<String>(2, "str2") // OK
    val p2_2: C<Int>.P2<String> = c.P2(2, "str2") // OK

    val tripleTA_0: C<Int>.TripleTA<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!> = c.TripleTA<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>(3, "str3", <!ARGUMENT_TYPE_MISMATCH!>'c'<!>) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
    val tripleTA_1: C<Int>.TripleTA<String, Char> = c.TripleTA<String, Char>(3, "str3", 'c') // OK
    val tripleTA_2: C<Int>.TripleTA<String, Char> = c.TripleTA(3, "str3", 'c') // OK

    val p3_0: C<Int>.P3<String> = c.P3("str4", 4) // OK
    val p3_1: C<Int>.P3<String> = c.P3<String>("str4", 4) // OK
    val p3_2: C<Int>.P3<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!> = c.P3<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>("str4", 4) // WRONG_NUMBER_OF_TYPE_ARGUMENTS

    val ta: C<Int>.TA = c.TA() // OK
    val ta2: C<Int>.TA2 = c.TA2() // OK

    val innerTA: C<Int>.InnerTA = c.InnerTA() // OK
    val innerTA2: C<Int>.InnerTA2<String> = c.InnerTA2<String>() // OK*/

    /*val inner_0 = c.Inner() // OK
    val innerTA_0 = c.InnerTA() // OK
    val innerTA2_1 = c.InnerTA2<String>() // OK*/
}
