package codebase.koog.governance

import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphModel
import com.cheroliv.graphify.model.GraphNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import contracts.session.AgentContext
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GovernanceContextLoaderTest {

    private val mapper = ObjectMapper().registerKotlinModule()

    private val syntheticGraph =
        GraphModel(
            nodes =
                listOf(
                    GraphNode("bakery/BakeryPlugin.adoc", "BakeryPlugin.adoc", "file", "bakery-gradle"),
                    GraphNode("bakery/SiteManager.adoc", "SiteManager.adoc", "file", "bakery-gradle"),
                    GraphNode("bakery-gradle", "bakery-gradle", "module", "bakery-gradle"),
                ),
            edges =
                listOf(
                    GraphEdge("bakery/BakeryPlugin.adoc", "bakery/SiteManager.adoc", "reference"),
                ),
            communities =
                listOf(
                    GraphCommunity("bakery-gradle", "Bakery Gradle Plugin", 2),
                ),
        )

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
    @Tag("integration")
    fun `loads real codebase governance files`() {
        val projectDir = File("/home/cheroliv/workspace/foundry/public/codebase-gradle/codebase-plugin")

        val ctx = GovernanceContextLoader().load(projectDir)

        assertTrue(ctx.eagerRules.isNotBlank(), "Should load real AGENT/PROMPT_REPRISE/INDEX")
        assertContains(ctx.eagerRules, "AGENT.adoc")
        assertTrue(ctx.backlogItems.isNotEmpty(), "Should extract backlog items")
    }

    @Test
    fun `graphRelations is empty when no graph file is provided`(@TempDir tempDir: File) {
        val ctx = GovernanceContextLoader().load(tempDir)

        assertEquals("", ctx.graphRelations)
    }

    @Test
    fun `graphRelations contains real subgraph content when graph file is provided`(@TempDir tempDir: File) {
        val graphFile = tempDir.resolve("graph.json")
        graphFile.writeText(mapper.writeValueAsString(syntheticGraph))

        val ctx = GovernanceContextLoader(graphFile = graphFile).load(tempDir)

        assertContains(ctx.graphRelations, "[Graphify] subgraph: 2 nodes, 1 edges, 1 communities")
        assertContains(ctx.graphRelations, "- node bakery/BakeryPlugin.adoc [type=file, community=bakery-gradle]")
        assertContains(ctx.graphRelations, "- edge bakery/BakeryPlugin.adoc -> bakery/SiteManager.adoc [type=reference]")
        assertContains(ctx.graphRelations, "- community bakery-gradle (2 nodes)")
    }

    @Test
    fun `graphRelations falls back to the missing file message when graph file does not exist`(@TempDir tempDir: File) {
        val ctx = GovernanceContextLoader(graphFile = tempDir.resolve("missing/graph.json")).load(tempDir)

        assertEquals("[Graphify] graph.json non trouve dans office/", ctx.graphRelations)
    }

    @Test
    fun `graphRelations falls back to the illisible message when graph file is malformed`(@TempDir tempDir: File) {
        val graphFile = tempDir.resolve("graph.json").apply { writeText("this is not valid json") }

        val ctx = GovernanceContextLoader(graphFile = graphFile).load(tempDir)

        assertTrue(ctx.graphRelations.startsWith("[Graphify] graph.json illisible:"))
    }
}
