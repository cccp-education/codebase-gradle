package codebase.koog.governance

import contracts.session.AgentContext
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GovernanceContextLoaderTest {

    @Test
    fun `loads AGENT adoc as eager rules`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent Rules\n\nRule 1\n")

        val ctx = GovernanceContextLoader().load(tempDir)

        assertContains(ctx.eagerRules, "= Agent Rules")
        assertContains(ctx.eagerRules, "Rule 1")
    }

    @Test
    fun `loads governance files from first level subproject`(@TempDir tempDir: File) {
        val subproject = File(tempDir, "codebase-plugin").apply { mkdirs() }
        File(subproject, "AGENT.adoc").writeText("= Sub Agent\n")
        File(subproject, "PROMPT_REPRISE.adoc").writeText("= Reprise\n")

        val ctx = GovernanceContextLoader().load(tempDir)

        assertContains(ctx.eagerRules, "= Sub Agent")
        assertContains(ctx.eagerRules, "= Reprise")
    }

    @Test
    fun `extracts backlog items from BACKLOG adoc`(@TempDir tempDir: File) {
        File(tempDir, "BACKLOG.adoc").writeText(
            """
            = Backlog
            
            * [ ] Open item
            * [x] Done item
            * [-] Other item
            """.trimIndent()
        )

        val ctx = GovernanceContextLoader().load(tempDir)

        assertEquals(3, ctx.backlogItems.size)
        assertTrue(ctx.backlogItems.any { it.contains("Open item") })
        assertTrue(ctx.backlogItems.any { it.contains("Done item") })
    }

    @Test
    fun `returns empty context when no governance files exist`(@TempDir tempDir: File) {
        val ctx = GovernanceContextLoader().load(tempDir)

        assertEquals("", ctx.eagerRules)
        assertEquals(emptyList<String>(), ctx.backlogItems)
    }

    @Test
    fun `loads real codebase governance files`() {
        val projectDir = File("/home/cheroliv/workspace/foundry/public/codebase-gradle/codebase-plugin")

        val ctx = GovernanceContextLoader().load(projectDir)

        assertTrue(ctx.eagerRules.isNotBlank(), "Should load real AGENT/PROMPT_REPRISE/INDEX")
        assertContains(ctx.eagerRules, "AGENT.adoc")
        assertTrue(ctx.backlogItems.isNotEmpty(), "Should extract backlog items")
    }
}
