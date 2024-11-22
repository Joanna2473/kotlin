
// SNIPPET

fun f() = "OK"

// SNIPPET

class C {
    val v1: String = f()
    val v2: String
        get() = f()
}

// SNIPPET

print("${C().v1}_${C().v2}")

// expected out: OK_OK
