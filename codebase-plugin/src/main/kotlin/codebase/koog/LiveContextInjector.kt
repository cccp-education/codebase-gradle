package codebase.koog

import codebase.koog.session.AgentContext
import contracts.vibecoding.registry.AuditEntry
import codebase.koog.state.VibecodingState

class LiveContextInjector {

    fun injectLiveContext(
        state: VibecodingState,
        auditEntries: List<AuditEntry>,
        staticContext: AgentContext? = null
    ): String {
        val liveSection = buildLiveSection(state, auditEntries)
        val staticSection = buildStaticSection(staticContext)

        if (liveSection.isBlank() && staticSection.isBlank()) return ""

        return buildString {
            if (liveSection.isNotBlank()) {
                appendLine("[LIVE_CONTEXT]")
                append(liveSection)
            }
            if (staticSection.isNotBlank()) {
                if (liveSection.isNotBlank()) appendLine()
                appendLine("[STATIC_CONTEXT]")
                append(staticSection)
            }
        }.trimEnd()
    }

    private fun buildLiveSection(state: VibecodingState, auditEntries: List<AuditEntry>): String {
        return buildString {
            appendLine("Intention: ${state.intention}")
            appendLine("Iteration: ${state.iteration + 1}/${state.maxActions}")
            appendLine("Dry run: ${state.dryRun}")
            val status = when {
                state.error != null && state.finished -> "ERROR"
                state.finished -> "FINISHED"
                state.error != null -> "RECOVERING"
                else -> "OK"
            }
            appendLine("Status: $status")
            if (state.error != null) {
                appendLine("Error: ${state.error}")
                if (state.retryCount > 0) {
                    appendLine("Retry: ${state.retryCount}/${state.maxRetries}")
                }
            }
            state.focusLevel?.let { appendLine("Focus: $it") }

            if (state.executedTasks.isNotEmpty()) {
                appendLine()
                appendLine("Executed tasks: ${state.executedTasks.joinToString(", ")}")
            }

            if (auditEntries.isNotEmpty()) {
                appendLine()
                appendLine("Tool call history:")
                auditEntries.forEach { entry ->
                    val args = if (entry.arguments.isNotEmpty()) {
                        entry.arguments.values.joinToString(", ")
                    } else ""
                    val resultPreview = entry.result.take(200)
                    val errorSuffix = entry.error?.let { " [ERROR: $it]" } ?: ""
                    appendLine("  - ${entry.tool}($args) → ${resultPreview}$errorSuffix")
                }
            }
        }
    }

    private fun buildStaticSection(staticContext: AgentContext?): String {
        if (staticContext == null) return ""
        val hasContent = staticContext.eagerRules.isNotBlank()
                || staticContext.ragChunks.isNotEmpty()
                || staticContext.graphRelations.isNotBlank()
                || staticContext.backlogItems.isNotEmpty()
        if (!hasContent) return "(none)"

        return buildString {
            if (staticContext.eagerRules.isNotBlank()) {
                appendLine("EAGER rules:")
                appendLine(staticContext.eagerRules)
            }
            if (staticContext.ragChunks.isNotEmpty()) {
                if (staticContext.eagerRules.isNotBlank()) appendLine()
                appendLine("RAG chunks:")
                staticContext.ragChunks.forEach { appendLine("  - $it") }
            }
            if (staticContext.graphRelations.isNotBlank()) {
                if (staticContext.eagerRules.isNotBlank() || staticContext.ragChunks.isNotEmpty()) appendLine()
                appendLine("Graph relations:")
                appendLine(staticContext.graphRelations)
            }
            if (staticContext.backlogItems.isNotEmpty()) {
                if (staticContext.eagerRules.isNotBlank() || staticContext.ragChunks.isNotEmpty() || staticContext.graphRelations.isNotBlank()) appendLine()
                appendLine("Backlog items:")
                staticContext.backlogItems.forEach { appendLine("  - $it") }
            }
        }
    }
}
