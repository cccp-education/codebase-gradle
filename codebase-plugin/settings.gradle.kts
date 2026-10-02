@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
    id("com.gradleup.nmcp.settings").version("1.5.0")
}

val globalProps = java.util.Properties().also {
    val globalFile = file(System.getProperty("user.home") + "/.gradle/gradle.properties")
    if (globalFile.exists()) it.load(globalFile.inputStream())
}

nmcpSettings {
    centralPortal {
        username = globalProps.getProperty("ossrhUsername") ?: ""
        password = globalProps.getProperty("ossrhPassword") ?: ""
        publishingType = "AUTOMATIC"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

// ── MEM-CAT — Catalog workspace publié (MEMPHIS) : pin unique par borough (D4) ──
// education.cccp:workspace-catalog:0.0.62 — source de vérité des versions
// cross-borough. Le borough ne bump que ce pin ; la version propre et le BOM
// platform viennent de ws.* (garde `CodebasePluginPublicationTest`). Pattern
// bakery (MEM-CAT-3), dernier consumer non migré (rollout S-022 ABIDJAN).
dependencyResolutionManagement {
    versionCatalogs {
        create("ws") {
            from("education.cccp:workspace-catalog:0.0.62")
        }
    }
}

rootProject.name = "codebase-plugin"
