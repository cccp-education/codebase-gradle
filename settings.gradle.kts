pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}
// GBL-014 — the publication channel is a build property, never a branch.
val publicationChannel = providers.gradleProperty("channel").getOrElse("jitpack")

if (publicationChannel == "jitpack") {
    // CNV-16 — JitPack mirror for the education.cccp.build.* conventions
    // (bypasses Sonatype / Gradle Plugin Portal). The JitPack tag is pinned while
    // the canonical plugin declarations may stay on the released version.
    pluginManagement.repositories.maven("https://jitpack.io")
    pluginManagement.resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("education.cccp.build.")) {
            useModule("com.github.cccp-education.conventions-gradle:conventions-plugin:0.0.18")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.9.0")
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "codebase-gradle"
if (publicationChannel == "jitpack") {
    dependencyResolutionManagement.repositories.maven("https://jitpack.io")
}
