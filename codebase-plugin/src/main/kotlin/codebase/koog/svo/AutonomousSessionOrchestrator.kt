package codebase.koog.svo

import codebase.koog.state.VibecodingState
import java.io.File
import java.time.Instant

/**
 * Outcome of one orchestrated autonomous run (EPIC SVO-4, D6 steps 5-6):
 * how many chained sessions executed, why the chain stopped, and the
 * generated resumption prompt (SVO-2 planner) with its on-disk location
 * (`build/vibecoding/`), ready for the next session opener.
 */
data class AutonomousSessionReport(
    val borough: String,
    val sessionsExecuted: Int,
    val stopReason: String,
    val resumptionPrompt: String,
    val resumptionPromptPath: String,
    val brainstormingIntention: String? = null,
    val backlogOpenItems: List<String> = emptyList(),
)

/**
 * Deterministic orchestrator for the `autonomousSession` task
 * (EPIC SVO-4, decisions D6/D7/D8/D9 — pure, no Gradle, no LLM):
 *
 * 1. resolve the session prompt (`SessionPromptResolver` — CLI > borough default);
 * 2. run one vibecoding session through the injected [runSession] lambda
 *    (production wiring: `VibecodingGraph.execute`; tests: a fake);
 * 3. parse the BACKLOG state (SVO-2 `BacklogDetector`);
 * 4. generate the resumption prompt (SVO-2 `SessionResumptionPlanner`) and
 *    write it under `build/vibecoding/`;
 * 5. chain while the backlog still has open items — bounded by
 *    [maxChainedSessions] (D9) and flipped to BRAINSTORMING when the
 *    backlog is complete (D8).
 *
 * The chain loop itself is pure: the Gradle task is a thin adapter.
 */
object AutonomousSessionOrchestrator {

    const val STOP_BRAINSTORMING: String = "brainstorming"
    const val STOP_MAX_SESSIONS: String = "maxChainedSessions"
    const val STOP_ERROR: String = "sessionError"

    fun orchestrate(
        borough: String,
        prompt: String?,
        projectDir: File,
        maxChainedSessions: Int,
        maxActionsPerSession: Int,
        runSession: (VibecodingState) -> String,
        auditAppend: (String) -> Unit,
    ): AutonomousSessionReport {
        var sessionsExecuted = 0
        var missionIntention = ""
        var stopReason = ""

        for (attempt in 1..maxChainedSessions) {
            // Attempt 1 resolves the mission prompt (CLI > borough default);
            // chained attempts re-resolve the borough default — the fake/loop
            // keeps consuming the backlog items it wrote.
            val resolved = SessionPromptResolver.resolve(
                cliPrompt = if (attempt == 1) prompt else null,
                borough = borough,
                projectDir = projectDir,
            )
            val intention = resolved.prompt
            if (attempt == 1) missionIntention = intention
            val state = VibecodingState(
                intention = intention,
                workspaceRoot = projectDir.absolutePath,
                dryRun = false,
                maxActions = maxActionsPerSession,
            )
            val result = try {
                runSession(state)
            } catch (e: Exception) {
                auditAppend(jsonLine(borough, attempt, intention, error = e.message))
                sessionsExecuted = attempt
                stopReason = STOP_ERROR
                break
            }
            auditAppend(jsonLine(borough, attempt, intention, error = null, result = result.take(200)))
            sessionsExecuted = attempt

            val items = BacklogDetector.parse(readBacklogLines(projectDir))
            if (!BacklogDetector.isComplete(items)) continue

            // D8: backlog complete → chain-up BRAINSTORMING session (D6 step 6),
            // still bounded by the D9 guard.
            stopReason = STOP_BRAINSTORMING
            val brainstormingIntention =
                "BRAINSTORM: vision of the $borough borough (autofocus BIG_PICTURE, cadrage global)"
            if (attempt < maxChainedSessions) {
                val brainstormState = VibecodingState(
                    intention = brainstormingIntention,
                    workspaceRoot = projectDir.absolutePath,
                    dryRun = false,
                    maxActions = maxActionsPerSession,
                )
                val brainstormResult = try {
                    runSession(brainstormState)
                } catch (e: Exception) {
                    auditAppend(jsonLine(borough, attempt + 1, brainstormingIntention, error = e.message))
                    stopReason = STOP_ERROR
                    break
                }
                auditAppend(jsonLine(borough, attempt + 1, brainstormingIntention, error = null, result = brainstormResult.take(200)))
                sessionsExecuted = attempt + 1
            }
            break
        }

        if (stopReason.isEmpty()) stopReason = STOP_MAX_SESSIONS

        val openItems = readBacklogLines(projectDir)
            .let(BacklogDetector::parse)
            .let(BacklogDetector::openItems)
            .map { it.summary() }

        val resumption = SessionResumptionPlanner.plan(
            SessionResumptionState(
                borough = borough,
                openItems = openItems,
                doneItems = emptyList(),
                lastIntention = missionIntention.take(200),
            )
        )
        val path = writeResumptionPrompt(projectDir, resumption)

        return AutonomousSessionReport(
            borough = borough,
            sessionsExecuted = sessionsExecuted,
            stopReason = stopReason,
            resumptionPrompt = resumption,
            resumptionPromptPath = path,
            brainstormingIntention = if (stopReason == STOP_BRAINSTORMING) {
                "BRAINSTORM: vision of the $borough borough (autofocus BIG_PICTURE, cadrage global)"
            } else null,
            backlogOpenItems = openItems,
        )
    }

    private fun readBacklogLines(projectDir: File): List<String> {
        val candidates = listOf(
            projectDir.resolve("BACKLOG.adoc"),
            projectDir.resolve(".agents/BACKLOG.adoc"),
        )
        val file = candidates.firstOrNull { it.isFile } ?: return emptyList()
        return try {
            file.readLines()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeResumptionPrompt(projectDir: File, prompt: String): String {
        val dir = File(projectDir, "build/vibecoding")
        dir.mkdirs()
        val file = File(dir, "resumption-prompt.adoc")
        file.writeText(prompt, Charsets.UTF_8)
        return file.absolutePath
    }

    private fun jsonLine(borough: String, session: Int, intention: String, error: String?, result: String? = null): String = buildString {
        append("{\"timestamp\":\"${Instant.now()}\"")
        append(",\"borough\":\"$borough\"")
        append(",\"session\":$session")
        append(",\"action\":\"autonomous_session\"")
        append(",\"intention\":\"${jsonlEscape(intention.take(120))}\"")
        if (result != null) append(",\"result\":\"${jsonlEscape(result.take(120))}\"")
        append(",\"error\":${error?.let { "\"${jsonlEscape(it)}\"" } ?: "null"}")
        append("}")
        appendLine()
    }

    private fun jsonlEscape(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}
