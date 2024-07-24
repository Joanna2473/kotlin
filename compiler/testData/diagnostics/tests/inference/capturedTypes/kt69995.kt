// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-69995

// FILE: Adder.java
public abstract class Adder<
        B extends Builder<B, Q, I, A>,
        Q extends Query<B, Q, I, A>,
        I extends Inserter<B, Q, I, A>,
        A extends Adder<B, Q, I, A>>
{}

// FILE: Inserter.java
public abstract class Inserter<
        B extends Builder<B, Q, I, A>,
        Q extends Query<B, Q, I, A>,
        I extends Inserter<B, Q, I, A>,
        A extends Adder<B, Q, I, A>>
{}

// FILE: Query.java
public abstract class Query<
        B extends Builder<B, Q, I, A>,
        Q extends Query<B, Q, I, A>,
        I extends Inserter<B, Q, I, A>,
        A extends Adder<B, Q, I, A>>
{}

// FILE: Builder.java
public abstract class Builder<
                B extends Builder<B, Q, I, A>,
        Q extends Query<B, Q, I, A>,
        I extends Inserter<B, Q, I, A>,
        A extends Adder<B, Q, I, A>>
{
    public abstract B add(String tag);
    public abstract Q build();
}

// FILE: main.kt
fun check(b: Builder<*, *, *, *>) {
    b.add("1").add("2").build()
}
