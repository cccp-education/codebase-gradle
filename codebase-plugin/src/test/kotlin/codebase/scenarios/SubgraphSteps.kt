package codebase.scenarios

import contracts.context.ContextChannel
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SubgraphSteps(private val world: SubgraphWorld) {

    @Given("a synthetic graph.json with 3 nodes, 1 edge and 1 community")
    fun `synthetic graph in office`() {
        world.ensureWorkspace()
        world.writeSyntheticGraph()
    }

    @Given("a workspace without a graph file")
    fun `workspace without graph file`() {
        world.ensureWorkspace()
        world.ensureNoGraphFile()
    }

    @When("I execute the multi-channel context graph")
    fun `execute multi channel graph`() {
        world.executeGraph()
    }

    @When("I load governance context with the graph file")
    fun `load governance with graph file`() {
        world.loadGovernanceWithGraphFile()
    }

    @Then("the Graphify channel contains the subgraph header")
    fun `graphify channel contains subgraph header`() {
        val channel = world.channel(ContextChannel.Graphify::class.java)
        assertNotNull(channel, "Graphify channel should be present")
        assertTrue(channel.content.contains("[Graphify] subgraph:"), "Should contain subgraph header. Content: ${channel.content.take(300)}")
    }

    @And("the Graphify channel contains a node from the synthetic graph")
    fun `graphify channel contains node`() {
        val channel = world.channel(ContextChannel.Graphify::class.java)
        assertNotNull(channel)
        assertTrue(channel.content.contains("bakery/BakeryPlugin.adoc"), "Should contain node. Content: ${channel.content.take(300)}")
    }

    @Then("the Resource channel contains the subgraph header")
    fun `resource channel contains subgraph header`() {
        val channel = world.channel(ContextChannel.Resource::class.java)
        assertNotNull(channel, "Resource channel should be present")
        assertTrue(channel.content.contains("[Graphify] subgraph:"), "Should contain subgraph header. Content: ${channel.content.take(300)}")
    }

    @And("the Resource channel contains a node from the synthetic graph")
    fun `resource channel contains node`() {
        val channel = world.channel(ContextChannel.Resource::class.java)
        assertNotNull(channel)
        assertTrue(channel.content.contains("bakery/BakeryPlugin.adoc"), "Should contain node. Content: ${channel.content.take(300)}")
    }

    @Then("graphRelations contains the subgraph header")
    fun `graphRelations contains subgraph header`() {
        val ctx = world.agentContext
        assertNotNull(ctx, "AgentContext should be loaded")
        assertTrue(
            ctx.graphRelations.contains("[Graphify] subgraph:"),
            "graphRelations should contain subgraph header. Content: ${ctx.graphRelations.take(300)}"
        )
    }

    @And("graphRelations contains a node from the synthetic graph")
    fun `graphRelations contains node`() {
        val ctx = world.agentContext
        assertNotNull(ctx)
        assertTrue(
            ctx.graphRelations.contains("bakery/BakeryPlugin.adoc"),
            "graphRelations should contain node. Content: ${ctx.graphRelations.take(300)}"
        )
    }

    @Then("the Graphify channel falls back to the missing file message")
    fun `graphify channel falls back`() {
        val channel = world.channel(ContextChannel.Graphify::class.java)
        assertNotNull(channel)
        assertTrue(
            channel.content.contains("non trouve") || channel.content.contains("not found"),
            "Should fall back to missing file message. Content: ${channel.content.take(300)}"
        )
    }

    @And("the Resource channel falls back to the missing file message")
    fun `resource channel falls back`() {
        val channel = world.channel(ContextChannel.Resource::class.java)
        assertNotNull(channel)
        assertTrue(
            channel.content.contains("non trouve") || channel.content.contains("not found"),
            "Should fall back to missing file message. Content: ${channel.content.take(300)}"
        )
    }
}