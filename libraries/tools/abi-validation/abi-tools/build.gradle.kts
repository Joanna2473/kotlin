plugins {
    kotlin("jvm")

    id("com.gradle.plugin-publish")
    `maven-publish`
}

dependencies {
    implementation(project(":tools:abi-tools-api"))

    implementation(project(":kotlin-metadata-jvm"))
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
}
