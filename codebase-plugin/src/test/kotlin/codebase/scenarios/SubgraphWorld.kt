package codebase.scenarios

import codebase.koog.MultiChannelContextGraph
import codebase.koog.MultiChannelState
import codebase.koog.governance.GovernanceContextLoader
import com.cheroliv.graphify.model.GraphCommunity
import com.cheroliv.graphify.model.GraphEdge
import com.cheroliv.graphify.model.GraphModel
import com.cheroliv.graphify.model.GraphNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import contracts.context.ChannelBudget
import contracts.context.ContextChannel
import contracts.session.AgentContext
import java.io.File
import java.nio.file.Files

class SubgraphWorld {

    var workspaceRoot: File = Files.createTempDirectory("subgraph-cucumber").toFile()
    var graphFileExists: Boolean = true
    var result: MultiChannelState? = null
    var agentContext: AgentContext? = null

    fun ensureWorkspace() {
        if (!workspaceRoot.exists()) {
            workspaceRoot = Files.createTempDirectory("subgraph-cucumber").toFile()
        }
    }

    fun writeSyntheticGraph() {
        val office = workspaceRoot.resolve("office").apply { mkdirs() }
        val mapper = ObjectMapper().registerKotlinModule()
        val model =
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
        office.resolve("graph.json").writeText(mapper.writeValueAsString(model))
        graphFileExists = true
    }

    fun ensureNoGraphFile() {
        val office = workspaceRoot.resolve("office").apply { mkdirs() }
        office.resolve("graph.json").delete()
        graphFileExists = false
    }

    fun executeGraph() {
        val graphFile = if (graphFileExists) workspaceRoot.resolve("office/graph.json") else null
        val graph = MultiChannelContextGraph(graphFile = graphFile)
        val state =
            MultiChannelState(
                intention = "test",
                workspaceRoot = workspaceRoot.absolutePath,
                budget =
                    ChannelBudget(
                        totalTokenBudget = 3000,
                        budgetEager = 0.35,
                        budgetRag = 0.25,
                        budgetGraphify = 0.15,
                        budgetDocs = 0.15,
                        budgetResource = 0.10,
                    ),
            )
        result = graph.execute(state)
    }

    fun loadGovernanceWithGraphFile() {
        val graphFile = workspaceRoot.resolve("office/graph.json")
        agentContext = GovernanceContextLoader(graphFile = graphFile).load(workspaceRoot)
    }

    fun channel(type: Class<out ContextChannel>): ContextChannel? =
        result?.channels?.find { type.isInstance(it) }
}