package codebase.koog.agentic

import contracts.vibecoding.registry.ToolRegistry
import java.io.File

/**
 * Câble automatiquement le hook d'interdiction issu de l'ingestion de gouvernance
 * dans un [ToolRegistry] existant.
 *
 * Responsabilité unique : exécuter l'ingestion des fichiers de gouvernance du projet
 * courant et, si des règles PRE_HOOK sont produites, retourner un [ToolRegistry]
 * dont les appels exec_shell / exec_gradle seront filtrés.
 *
 * Cet objet est testable unitairement car il ne dépend pas du Gradle Project.
 */
object GovernanceEnforcementWirer {

    fun wire(
        toolRegistry: ToolRegistry,
        workspaceRoot: File,
        validator: ChunkValidator = ChunkValidator()
    ): ToolRegistry {
        val result = GovernanceIngestor(validator).ingest(workspaceRoot)
        val executor = result.executor ?: return toolRegistry

        return ToolRegistry(
            enforcementHook = { toolName, arguments ->
                val check = executor.check(toolName, arguments)
                if (check.allowed) null else formatReason(check)
            }
        ).also { wired ->
            for (name in toolRegistry.toolNames()) {
                wired.register(toolRegistry.get(name))
            }
        }
    }

    private fun formatReason(result: ExecutionResult): String =
        listOfNotNull(
            result.reason,
            result.ruleId?.let { "ruleId=$it" }
        ).joinToString(" | ")
}
