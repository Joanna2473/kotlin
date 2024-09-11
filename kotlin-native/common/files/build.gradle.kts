import org.jetbrains.kotlin.konan.target.TargetWithSanitizer

plugins {
    id("compile-to-bitcode")
    kotlin("jvm")
    id("native-interop-plugin")
}

bitcode {
    hostTarget {
        module("files") {
            sourceSets {
                main {
                    headersDirs.from("include")
                    inputFiles.from("src")
                }
            }
        }
    }
}

kotlinNativeInterop {
    create("files") {
        pkg("org.jetbrains.kotlin.backend.konan.files")
        linker("clang++")
        linkOutputs(bitcode.hostTarget.module("files").get().sourceSets.main.get().task.get())
        headers(layout.projectDirectory.files("include/Files.h"))
    }
}

configurations.apiElements.configure {
    extendsFrom(kotlinNativeInterop["files"].configuration)
}

configurations.runtimeElements.configure {
    extendsFrom(kotlinNativeInterop["files"].configuration)
}

val nativeLibs by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE)
        attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, TargetWithSanitizer.host)
    }
}

artifacts {
    add(nativeLibs.name, layout.buildDirectory.dir("nativelibs/${TargetWithSanitizer.host}")) {
        builtBy(kotlinNativeInterop["files"].genTask)
    }
}