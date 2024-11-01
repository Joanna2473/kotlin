plugins {
    kotlin("jvm")

    id("com.gradle.plugin-publish")
    `maven-publish`
}

dependencies {
    compileOnly(kotlinStdlib())
}
