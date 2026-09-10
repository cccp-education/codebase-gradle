package codebase.koog.svo

import codebase.koog.governance.GovernanceContextLoader
import java.io.File

/**
 * Where the autonomous-session prompt came from (EPIC SVO-4, D6):
 * [TRANSFERRED] — the CLI owner passed `--prompt`; [BOROUGH_DEFAULT] —
 * blank/absent CLI prompt, the borough governance files feed the session.
 */
enum class SessionPromptSource { TRANSFERRED, BOROUGH_DEFAULT }

/**
 * Resolved session prompt + its provenance (EPIC SVO-4, D6 step 1).
 */
data class ResolvedSessionPrompt(
    val prompt: String,
    val source: SessionPromptSource,
)

/**
 * Pure prompt resolution for the `autonomousSession` task (EPIC SVO-4, D6):
 * a transferred CLI prompt wins; otherwise the borough default is built
 * from the governance EAGER files (PROMPT_REPRISE + BACKLOG open items via
 * [GovernanceContextLoader]) — the same fallback a human session opener
 * reads by hand.
 *
 * Pure function: file I/O only on the caller-provided [projectDir], no
 * Gradle, no LLM — unit-testable (pattern `BacklogDetector`).
 */
object SessionPromptResolver {

    fun resolve(
        cliPrompt: String?,
        borough: String,
        projectDir: File?,
    ): ResolvedSessionPrompt {
        if (!cliPrompt.isNullOrBlank()) {
            return ResolvedSessionPrompt(prompt = cliPrompt, source = SessionPromptSource.TRANSFERRED)
        }
        val governance = projectDir?.takeIf { it.isDirectory }?.let { dir ->
            try {
                GovernanceContextLoader().load(dir)
            } catch (_: Exception) {
                null
            }
        }
        return ResolvedSessionPrompt(
            prompt = buildDefaultPrompt(
                borough = borough,
                backlogItems = governance?.backlogItems.orEmpty(),
                promptReprise = projectDir?.let { readPromptReprise(it) }.orEmpty(),
            ),
            source = SessionPromptSource.BOROUGH_DEFAULT,
        )
    }

    /**
     * Reads PROMPT_REPRISE.adoc from the project root or a direct
     * subdirectory (mirror of [GovernanceContextLoader.resolveFile] —
     * multi-module Gradle layouts), truncated to the same 2000-char budget
     * as `CompositeContextBuilder.buildScoped`.
     */
    private fun readPromptReprise(projectDir: File): String {
        val candidates = buildList {
            add(projectDir.resolve("PROMPT_REPRISE.adoc"))
            projectDir.listFiles()?.filter { it.isDirectory }?.forEach {
                add(File(it, "PROMPT_REPRISE.adoc"))
            }
        }
        return candidates.firstOrNull { it.isFile }
            ?.let { file ->
                runCatching { file.readText(Charsets.UTF_8).take(PROMPT_REPRISE_BUDGET) }.getOrNull()
            }
            .orEmpty()
    }

    private fun buildDefaultPrompt(
        borough: String,
        backlogItems: List<String>,
        promptReprise: String,
    ): String = buildString {
        appendLine("= Autonomous session — $borough")
        appendLine()
        appendLine("Default borough prompt: work the backlog, first open item first.")
        appendLine("Objective: keep the borough backlog shrinking; stop when the")
        appendLine("backlog is complete (Brainstorming chain-up is handled by the orchestrator).")
        if (promptReprise.isNotBlank()) {
            appendLine()
            appendLine("--- PROMPT_REPRISE.adoc ---")
            appendLine(promptReprise)
            appendLine("--- end PROMPT_REPRISE.adoc ---")
        }
        if (backlogItems.isNotEmpty()) {
            appendLine()
            appendLine("Backlog open items:")
            backlogItems.forEachIndexed { index, item ->
                appendLine("  ${index + 1}. $item")
            }
        } else {
            appendLine()
            appendLine("No open backlog item detected — treat this session as an")
            appendLine("augmented-context brainstorming on the borough vision.")
        }
    }

    private const val PROMPT_REPRISE_BUDGET = 2_000
}
