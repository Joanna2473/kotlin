// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
public class A() {
    public val <!REDECLARATION!>FOO<!>: String = "test"

    public class <!REDECLARATION!>FOO<!>() { }
}

public class B() {
   companion object {
      public val <!REDECLARATION!>FOO<!>: String = "test"
      
      public class <!REDECLARATION!>FOO<!>() { }
   }
}
