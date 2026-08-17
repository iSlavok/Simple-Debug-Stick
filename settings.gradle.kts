pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie" }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"
    create(rootProject) {
        // One jar per anchor; each covers a patch band via the version range in
        // fabric.mod.json. Split points follow the APIs the source branches on:
        // DataComponents (1.20.5), the debug-stick interaction signature (1.21.2),
        // the Java-level bumps, and the obfuscated -> unobfuscated boundary (26+).
        versions("1.18.2", "1.19.4", "1.20.4", "1.20.6", "1.21.1", "1.21.8", "1.21.10")
        // Minecraft 26+ ships unobfuscated and needs JDK 25 + a non-remapping
        // toolchain, so it lives in its own build script and is only registered when
        // the running JDK can build it (keeps the project buildable on 17/21/24).
        if (JavaVersion.current().majorVersion.toInt() >= 25) {
            version("26.1.2", "26.1.2").buildscript = "build.unobfuscated.gradle.kts"
            version("26.2", "26.2").buildscript = "build.unobfuscated.gradle.kts"
        }
        vcsVersion = "1.21.8"
    }
}

rootProject.name = "simple-debug-stick"
