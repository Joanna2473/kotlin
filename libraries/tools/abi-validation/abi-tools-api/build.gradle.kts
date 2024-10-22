import plugins.configureDefaultPublishing

plugins {
    kotlin("jvm")
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}
