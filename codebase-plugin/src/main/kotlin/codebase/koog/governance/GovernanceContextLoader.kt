package codebase.koog.governance

import codebase.koog.session.AgentContext
import java.io.File

/**
 * Charge les fichiers de gouvernance agentique textuels (EAGER) d'un projet
 * et les expose dans un [AgentContext] utilisable par la boucle vibecoding locale.
 *
 * Fichiers recherchés (par ordre de priorité) :
 * - AGENT.adoc
 * - .agents/INDEX.adoc
 * - PROMPT_REPRISE.adoc
 * - .agents/SESSIONS_HISTORY.adoc
 * - .agents/TEST_COVERAGE_ANALYSIS.adoc
 * - BACKLOG.adoc (racine ou sous-module)
 *
 * Le loader cherche à la racine du projet puis dans le premier niveau
 * de sous-répertoires (ex: codebase-plugin/ pour les plugins Gradle).
 */
class GovernanceContextLoader {

    companion object {
        val EAGER_FILE_NAMES = listOf(
            "AGENT.adoc",
            ".agents/INDEX.adoc",
            "PROMPT_REPRISE.adoc",
            ".agents/SESSIONS_HISTORY.adoc",
            ".agents/TEST_COVERAGE_ANALYSIS.adoc",
        )

        val BACKLOG_FILE_NAMES = listOf(
            "BACKLOG.adoc",
            ".agents/BACKLOG.adoc",
        )
    }

    fun load(projectDir: File): AgentContext {
        require(projectDir.isDirectory) { "Project dir must be a directory: $projectDir" }

        val eagerRules = buildString {
            EAGER_FILE_NAMES.forEach { relativePath ->
                val file = resolveFile(projectDir, relativePath)
                if (file != null && file.exists()) {
                    appendLine("== ${file.name} ==")
                    appendLine(file.readText(Charsets.UTF_8))
                    appendLine()
                }
            }
        }.trim()

        val backlogItems = BACKLOG_FILE_NAMES.mapNotNull { relativePath ->
            resolveFile(projectDir, relativePath)
        }.filter { it.exists() }
            .flatMap { file ->
                file.readText(Charsets.UTF_8)
                    .lines()
                    .filter { line ->
                        line.startsWith("* [") || line.startsWith("* [x]") || line.startsWith("* [ ]")
                    }
                    .map { it.trim() }
            }
            .distinct()

        return AgentContext(
            eagerRules = eagerRules,
            backlogItems = backlogItems,
            graphRelations = "",
            ragChunks = emptyList()
        )
    }

    /**
     * Résout un chemin relatif soit à la racine, soit dans un sous-répertoire
     * direct (utile pour les multi-modules Gradle où AGENT/INDEX sont dans
     * le sous-projet plugin).
     */
    private fun resolveFile(projectDir: File, relativePath: String): File? {
        val rootCandidate = File(projectDir, relativePath)
        if (rootCandidate.exists()) return rootCandidate

        projectDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
            val subCandidate = File(subDir, relativePath)
            if (subCandidate.exists()) return subCandidate
        }

        return null
    }
}
