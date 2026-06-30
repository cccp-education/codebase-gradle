package codebase.koog

import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import codebase.koog.state.AugmentedState
import ai.koog.agents.core.agent.asMermaidDiagram
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.ToolSelectionStrategy
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import codebase.rag.CompositeContextBuilder
import codebase.rag.EmbeddingPipeline
import codebase.rag.PgVectorConfig
import codex.store.CodexVectorStore
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File

class KoogPlanningOrchestrator {

    private val log = LoggerFactory.getLogger(KoogPlanningOrchestrator::class.java)

    val graph: AIAgentGraphStrategy<AugmentedState, AugmentedState> = strategy<AugmentedState, AugmentedState>(
        name = "augmented-planning",
        toolSelectionStrategy = ToolSelectionStrategy.NONE
    ) {
        val buildContext by node<AugmentedState, AugmentedState> { state ->
            buildContextNode(state)
        }

        val classify by node<AugmentedState, AugmentedState> { state ->
            classifyNode(state)
        }

        val plan by node<AugmentedState, AugmentedState> { state ->
            planNode(state)
        }

        edge(nodeStart forwardTo buildContext onCondition { _ -> true } transformed { it })
        edge(buildContext forwardTo classify onCondition { _ -> true } transformed { it })
        edge(classify forwardTo plan onCondition { _ -> true } transformed { it })
        edge(plan forwardTo nodeFinish onCondition { _ -> true } transformed { it })
    }

    fun execute(initialState: AugmentedState): AugmentedState {
        var state = try {
            buildContextNode(initialState)
        } catch (e: Exception) {
            log.debug("[KoogPlanningOrchestrator] buildContext failed (pgvector down?): {}", e.message)
            initialState.copy(compositeContext = null, error = "BuildContextFailed: ${e.message}")
        }

        state = try {
            classifyNode(state)
        } catch (e: Exception) {
            log.warn("[KoogPlanningOrchestrator] classify failed: {}", e.message)
            state.copy(classification = "simple", error = state.error ?: "ClassifyFailed: ${e.message}")
        }

        state = try {
            planNode(state)
        } catch (e: Exception) {
            log.error("[KoogPlanningOrchestrator] plan failed: {}", e.message)
            state.copy(
                planError = "PlanExecutionFailed: ${e.message}",
                error = "PlanExecutionFailed: ${e.message}"
            )
        }

        return state
    }

    fun planFeature(initialState: AugmentedState): AugmentedState {
        val enhancedState = initialState.copy(
            intention = "FEATURE: ${initialState.intention}"
        )
        return execute(enhancedState)
    }

    fun planArchitecture(initialState: AugmentedState): AugmentedState {
        val enhancedState = initialState.copy(
            intention = "ARCHITECTURE: ${initialState.intention}"
        )
        return execute(enhancedState)
    }

    fun planRefactor(initialState: AugmentedState): AugmentedState {
        val enhancedState = initialState.copy(
            intention = "REFACTOR: ${initialState.intention}"
        )
        return execute(enhancedState)
    }

    fun planDocumentation(initialState: AugmentedState): AugmentedState {
        val enhancedState = initialState.copy(
            intention = "DOCUMENTATION: ${initialState.intention}"
        )
        return execute(enhancedState)
    }

    fun asMermaidDiagram(): String = runBlocking { graph.asMermaidDiagram() }

    private fun buildContextNode(state: AugmentedState): AugmentedState {
        val context = buildCompositeContext(state.workspaceRoot, state.intention)
        val partialContext = context.ragSection.contains("indisponible", ignoreCase = true) ||
            context.docsSection.contains("indisponible", ignoreCase = true)
        return state.copy(
            compositeContext = context,
            error = if (partialContext) "ContextBuildPartial" else null
        )
    }

    private fun classifyNode(state: AugmentedState): AugmentedState {
        val classification = if (state.compositeContext != null) {
            classifyIntention(state.intention)
        } else {
            "simple"
        }
        return state.copy(classification = classification)
    }

    private fun planNode(state: AugmentedState): AugmentedState {
        val ctx = state.compositeContext
        return if (ctx == null) {
            state.copy(
                planError = "CompositeContext unavailable — cannot generate plan",
                error = "ContextBuildFailed"
            )
        } else {
            val planState = codebase.rag.PlannerIntegration.plan(state.intention, ctx)
            state.copy(
                planJson = planState.planJson,
                plan = planState.plan,
                planError = planState.error,
                error = state.error ?: planState.error
            )
        }
    }

    private fun buildCompositeContext(workspaceRoot: String, question: String): CompositeContext {
        val rootDir = File(workspaceRoot)
        val config = CompositeContextConfig()
        val cfg = PgVectorConfig.fromEnv()
        val store = cfg.toVectorStore()

        try {
            store.initSchema()
        } catch (e: Exception) {
            log.debug("[buildCompositeContext] pgvector schema init failed: {}", e.message)
        }

        val pipeline = EmbeddingPipeline(store)
        val codexStore = CodexVectorStore()
        val builder = CompositeContextBuilder(rootDir, store, pipeline, config, codexStore)
        return builder.build(question)
    }

    private fun classifyIntention(intention: String): String {
        return if (intention.length > 80 ||
            intention.contains("cross-borough", ignoreCase = true) ||
            intention.contains("multi-plugins", ignoreCase = true) ||
            intention.contains("architecture", ignoreCase = true)
        ) {
            "complexe"
        } else {
            "simple"
        }
    }
}
