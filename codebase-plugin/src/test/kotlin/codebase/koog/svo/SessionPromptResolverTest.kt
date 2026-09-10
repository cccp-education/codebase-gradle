package codebase.koog.svo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionPromptResolverTest {

    @Test
    fun `transferred CLI prompt wins over borough default`() {
        val resolved = SessionPromptResolver.resolve(
            cliPrompt = "Work the SVO-4 task now",
            borough = "codebase",
            projectDir = null,
        )
        assertEquals("Work the SVO-4 task now", resolved.prompt)
        assertEquals(SessionPromptSource.TRANSFERRED, resolved.source)
    }

    @Test
    fun `blank CLI prompt falls back to borough default`() {
        val resolved = SessionPromptResolver.resolve(
            cliPrompt = "   ",
            borough = "codebase",
            projectDir = null,
        )
        assertEquals(SessionPromptSource.BOROUGH_DEFAULT, resolved.source)
    }

    @Test
    fun `borough default embeds PROMPT_REPRISE content when file exists`() {
        val dir = java.nio.file.Files.createTempDirectory("svo-prompt").toFile()
        dir.resolve("PROMPT_REPRISE.adoc").writeText("= Prompt de Reprise — Session 259")
        val resolved = SessionPromptResolver.resolve(
            cliPrompt = null,
            borough = "codebase",
            projectDir = dir,
        )
        assertTrue(resolved.prompt.contains("Prompt de Reprise"), "default must embed PROMPT_REPRISE")
        assertEquals(SessionPromptSource.BOROUGH_DEFAULT, resolved.source)
    }

    @Test
    fun `borough default carries backlog open items`() {
        val dir = java.nio.file.Files.createTempDirectory("svo-backlog").toFile()
        dir.resolve("BACKLOG.adoc").writeText(
            """
            == EPIC SVO 🟡 EN COURS
            * [ ] SVO-4 task autonomousSession
            * [x] SVO-3 adapter planner
            """.trimIndent()
        )
        val resolved = SessionPromptResolver.resolve(
            cliPrompt = null,
            borough = "codebase",
            projectDir = dir,
        )
        assertTrue(resolved.prompt.contains("SVO-4 task autonomousSession"), "open backlog item must feed the default prompt")
    }

    @Test
    fun `no files at all yields a non blank fallback prompt naming the borough`() {
        val dir = java.nio.file.Files.createTempDirectory("svo-empty").toFile()
        val resolved = SessionPromptResolver.resolve(
            cliPrompt = null,
            borough = "queens-borough",
            projectDir = dir,
        )
        assertTrue(resolved.prompt.isNotBlank())
        assertTrue(resolved.prompt.contains("queens-borough"))
        assertEquals(SessionPromptSource.BOROUGH_DEFAULT, resolved.source)
    }

    @Test
    fun `null project dir keeps the resolution non crashing`() {
        val resolved = SessionPromptResolver.resolve(
            cliPrompt = null,
            borough = "codebase",
            projectDir = null,
        )
        assertTrue(resolved.prompt.isNotBlank())
    }
}
