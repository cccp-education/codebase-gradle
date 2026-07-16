package codebase.publishing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import java.io.File

class VersionCatalogTransitiveVersionsTest {

    private val tomlFile: File by lazy {
        val projectDir = File(System.getProperty("user.dir"))
        val rootDir = if (projectDir.name == "codebase-plugin") projectDir.parentFile else projectDir
        File(rootDir, "gradle/libs.versions.toml")
    }

    private val tomlContent: String by lazy { tomlFile.readText() }

    private val transitiveArtifacts = listOf(
        "agent-contracts",
        "codebase-contracts",
        "llm-pool-contracts",
        "opencode-session-contracts",
        "i18n-contracts",
        "codex-plugin",
    )

    @Test
    fun `version catalog file exists`() {
        assertTrue(tomlFile.exists(), "libs.versions.toml must exist at ${tomlFile.absolutePath}")
    }

    @Test
    fun `all transitive education cccp artifacts have explicit versions`() {
        for (artifact in transitiveArtifacts) {
            val matchingLines = tomlContent.lines().filter { it.contains(artifact) && it.contains("education.cccp") }
            assertTrue(
                matchingLines.isNotEmpty(),
                "Artifact '$artifact' not found in libs.versions.toml."
            )
            for (line in matchingLines) {
                assertTrue(
                    line.contains("version"),
                    "Artifact '$artifact' must have an explicit version in libs.versions.toml. " +
                        "Without it, the POM publishes the dependency without <version>, breaking transitive consumers. " +
                        "Line: $line"
                )
            }
        }
    }
}