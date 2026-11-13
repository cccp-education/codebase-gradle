package codebase.publishing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Publication hygiene guard (S-273 — MEM-CAT rollout, pattern
 * `BakeryPluginPublicationTest` S-244). Kills the split-brain class of bug that
 * left the self-application pinned at `0.0.10` (build) / `0.0.11` (toml) while
 * the real HEAD was `0.0.17`:
 *
 * - the plugin version derives from the *published workspace catalog* (`ws`),
 *   never a duplicated literal ;
 * - the local toml self-version equals the catalog self-version ;
 * - the `workspace-bom` platform pin is dynamic (no stale literal — piège #13).
 *
 * The published catalog versions are *injected by Gradle* (`ws.versions.*`),
 * never read from a neighbour repository's working tree (racy between sessions,
 * absent from an isolated checkout — bakery S-243 / graphify D5-RACE S-029). A
 * missing property is an explicit error, never a silent green.
 */
class CodebasePluginPublicationTest {
    private val pluginDir = File(System.getProperty("user.dir"))

    private fun publishedCodebaseVersion(): String = requireProperty(CODEBASE_VERSION_PROPERTY)

    private fun publishedBomVersion(): String = requireProperty(BOM_VERSION_PROPERTY)

    private fun requireProperty(name: String): String {
        val value = System.getProperty(name)
        if (value.isNullOrBlank()) {
            error("system property '$name' is not set — the publication guard must be injected by Gradle (ws.versions.*)")
        }
        return value
    }

    @Test
    fun `plugin version derives from the published workspace catalog`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)

        assertTrue(
            buildScript.withoutWhitespace().contains("version=ws.versions.codebase.plugin.get()"),
            "build.gradle.kts version must derive from the published workspace catalog (ws.versions.codebase.plugin)",
        )
    }

    @Test
    fun `plugin catalog self-version matches the published catalog version`() {
        val pluginCatalogVersion = codebaseVersionFrom(pluginDir.resolve("gradle/libs.versions.toml").readText(UTF_8))
        val wsCatalogVersion = publishedCodebaseVersion()

        assertEquals(
            wsCatalogVersion,
            pluginCatalogVersion,
            "plugin catalog codebase version ($pluginCatalogVersion) must match ws catalog codebase version ($wsCatalogVersion)",
        )
    }

    @Test
    fun `workspace bom platform pin matches the injected published bom version`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)
        val wsBomVersion = publishedBomVersion()

        // CNV-16 — the JitPack BOM POM is not a valid *platform* (rewritten as
        // plain dependencies, no module metadata): the platform stays on the last
        // Maven Central release, so a literal is legitimate. The guard is now an
        // anti-drift check: every `workspace-bom:<version>` pin must equal the
        // version injected from the single source of truth.
        val literals =
            Regex("""workspace-bom:([0-9][0-9A-Za-z.\-]*)""")
                .findAll(buildScript)
                .map { it.groupValues[1] }
                .toSet()

        assertEquals(
            setOf(wsBomVersion),
            literals,
            "every workspace-bom platform pin must equal the injected published BOM version ($wsBomVersion)",
        )
    }

    @Test
    fun `plugin group and id are stable for publication`() {
        val buildScript = pluginDir.resolve("build.gradle.kts").readText(UTF_8)
        val pluginId =
            pluginDir
                .resolve("gradle/libs.versions.toml")
                .readText(UTF_8)
                .lineSequence()
                .first { it.contains("id = \"education.cccp.codebase\"") }
                .substringAfter("id = \"")
                .substringBefore("\"")

        assertTrue(buildScript.contains("group = \"education.cccp\""))
        assertEquals("education.cccp.codebase", pluginId)
    }

    private fun codebaseVersionFrom(content: String): String =
        content
            .lineSequence()
            .map { it.substringBefore('#').trim() }
            .first { it.startsWith("codebase-plugin =") }
            .substringAfter("\"")
            .substringBefore("\"")

    private fun String.withoutWhitespace(): String = filterNot { it.isWhitespace() }

    private companion object {
        const val CODEBASE_VERSION_PROPERTY = "codebase.publishedCatalog.codebaseVersion"
        const val BOM_VERSION_PROPERTY = "codebase.publishedCatalog.bomVersion"
    }
}
