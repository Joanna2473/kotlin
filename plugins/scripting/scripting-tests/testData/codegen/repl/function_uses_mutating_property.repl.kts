
// SNIPPET

var v = "O"

// SNIPPET

fun f() = v

// SNIPPET

val o = f()

// SNIPPET

v = "K"

// SNIPPET

print("$o${f()}")

// expected out: OK
