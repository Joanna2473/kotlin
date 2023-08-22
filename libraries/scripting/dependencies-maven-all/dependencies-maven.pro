-target 1.8
-dontoptimize
-dontobfuscate
# -dontshrink

-keepdirectories META-INF/**

-dontnote **
-dontwarn org.jetbrains.kotlin.**
-dontwarn kotlin.script.experimental.**
-dontwarn junit.framework.**
-dontwarn afu.org.checkerframework.**
-dontwarn org.checkerframework.**
-dontwarn org.testng.**
-dontwarn org.osgi.**
-dontwarn javax.el.**
-dontwarn javax.crypto.**
-dontwarn javax.interceptor.**
-dontwarn org.eclipse.sisu.**
-dontwarn org.slf4j.**

-keep class kotlin.script.experimental.** { *; }

-keep class org.eclipse.sisu.** { *; }
-keep class org.jetbrains.kotlin.org.eclipse.sisu.** { *; }

-keep class com.google.inject.** { *; }
-keep class org.jetbrains.kotlin.com.google.inject.** { *; }

-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <init>(...);
}

-keep class org.jetbrains.kotlin.script.util.impl.PathUtilKt { *; }

-keep class com.google.common.** { *; }
-keep class org.jetbrains.kotlin.com.google.common.** { *; }

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    **[] values();
}