pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.terraformersmc.com/releases/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    create(rootProject) {
        versions("1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7")
        // MC 26.x is un-obfuscated and runs on the JDK-25 toolchain — it cannot use the
        // normal fabric-loom-remap + Mojang-mappings path, so it gets its own build script.
        versions("26.2", "26.1.2", "26.1.1", "26.1").buildscript("build.unobf.gradle.kts")
        vcsVersion = "26.2"
    }
}

rootProject.name = "OneClickAnvil"
