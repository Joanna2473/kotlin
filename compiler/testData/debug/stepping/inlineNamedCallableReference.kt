
// FILE: test.kt
fun box() {
    var x = false
    f(::g)
}

inline fun f(block: () -> Unit) {
    block()
}

fun g() {}

// EXPECTATIONS JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:9 box
// test.kt:5 box
// test.kt:12 g
// test.kt:9 box
// test.kt:10 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:12 g
// test.kt:6 box

// EXPECTATIONS WASM
// test.kt:3 $box (10)
// test.kt:4 $box (12)
// test.kt:5 $box (4)
// test.kt:8 $box (32)
// test.kt:9 $box (4)
// test.kt:12 $g (8, 10)
// test.kt:10 $box (1)
// test.kt:6 $box (1)
