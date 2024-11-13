/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package cases.java;

class Part1 {
    public static void publicMethod(int param) { }

    public static class Part2 extends Part1 {
        public static void publicMethod(String param) { }
    }
}


public class Facade extends Part1.Part2 { }
