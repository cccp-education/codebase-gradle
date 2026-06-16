plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
    kotlin("plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.kotlinx.kover") version "0.9.8" apply false
    id("com.gradle.plugin-publish") version "2.1.0" apply false
}

allprojects {
    group = "education.cccp"
    version = "0.0.2"

    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
