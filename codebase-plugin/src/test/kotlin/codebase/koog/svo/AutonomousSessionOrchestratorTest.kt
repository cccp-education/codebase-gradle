package codebase.koog.svo

import codebase.koog.session.SessionRecord
import codebase.koog.session.SessionRepository
import codebase.koog.state.VibecodingState
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * In-memory [SessionRepository] fake for the orchestrator tests
 * (pattern `FakeSessionRepository` DashboardTest).
 */
class RecordingSessionRepository : SessionRepository {
    val created = mutableListOf<VibecodingState>()

    override suspend fun createSession(state: VibecodingState, confidentialityLevel: String): String {
        created += state
        return "session-${created.size}"
    }

    override suspend fun listSessions(limit: Int): List<SessionRecord> = emptyList()
}

class AutonomousSessionOrchestratorTest {

    private fun tempDir(): File = java.nio.file.Files.createTempDirectory("svo-orch").toFile()

    @Test
    fun `mission session runs the transferred prompt and chains brainstorming when backlog absent`() {
        val dir = tempDir()
        val executed = mutableListOf<String>()
        val repo = RecordingSessionRepository()
        val report = AutonomousSessionOrchestrator.orchestrate(
            borough = "codebase",
            prompt = "Work SVO-4",
            projectDir = dir,
            maxChainedSessions = 3,
            maxActionsPerSession = 1,
            runSession = { state ->
                repo.created += state
                "executed: ${state.intention}"
            },
            auditAppend = { line -> dir.resolve("audit.trace").appendText(line) },
        )
        // D8: no backlog file at all → backlog "complete" → BRAINSTORM chain-up
        assertEquals(2, repo.created.size, "mission session + chained brainstorming session")
        assertEquals("Work SVO-4", repo.created.first().intention, "transferred prompt feeds the first loop")
        assertTrue(
            repo.created.last().intention.contains("BRAINSTORM"),
            "second session runs the brainstorming intention (D8)",
        )
        assertTrue(report.resumptionPrompt.isNotBlank(), "resumption prompt must be generated")
        assertEquals(2, report.sessionsExecuted)
        assertEquals(AutonomousSessionOrchestrator.STOP_BRAINSTORMING, report.stopReason)
    }

    @Test
    fun `maxChainedSessions bounds the loop — stops with reason`() {
        val dir = tempDir()
        var counter = 0
        val report = AutonomousSessionOrchestrator.orchestrate(
            borough = "codebase",
            prompt = null,
            projectDir = dir,
            maxChainedSessions = 3,
            maxActionsPerSession = 5,
            runSession = { _ ->
                counter++
                dir.resolve("BACKLOG.adoc").writeText(
                    "== EPIC Loop 🟡 EN COURS\n* [ ] endless item $counter"
                )
                "done $counter"
            },
            auditAppend = {},
        )
        assertEquals(3, report.sessionsExecuted, "D9: chain stops at maxChainedSessions")
        assertEquals(AutonomousSessionOrchestrator.STOP_MAX_SESSIONS, report.stopReason)
    }

    @Test
    fun `complete backlog chains the brainstorming session and reports its intention`() {
        val dir = tempDir()
        dir.resolve("BACKLOG.adoc").writeText(
            "== EPIC Done ✅ TERMINÉ\n* [x] everything is done"
        )
        val executed = mutableListOf<String>()
        val report = AutonomousSessionOrchestrator.orchestrate(
            borough = "codebase",
            prompt = "one shot",
            projectDir = dir,
            maxChainedSessions = 3,
            maxActionsPerSession = 5,
            runSession = { state -> executed += state.intention; "done" },
            auditAppend = {},
        )
        assertEquals(2, report.sessionsExecuted, "mission + brainstorming chain-up (D8)")
        assertEquals(AutonomousSessionOrchestrator.STOP_BRAINSTORMING, report.stopReason)
        assertTrue(
            report.brainstormingIntention?.contains("codebase") == true,
            "brainstorm intention names the borough",
        )
        assertTrue(
            executed.last().contains("BRAINSTORM") && executed.last().contains("codebase"),
            "brainstorming session executes the reported intention",
        )
    }

    @Test
    fun `resumption prompt after backlog-consuming session lists remaining items`() {
        val dir = tempDir()
        dir.resolve("BACKLOG.adoc").writeText(
            "== EPIC SVO 🟡 EN COURS\n* [ ] SVO-4 task\n* [ ] SVO-5 chain-up\n* [x] SVO-3 done"
        )
        val report = AutonomousSessionOrchestrator.orchestrate(
            borough = "codebase",
            prompt = "work SVO-4",
            projectDir = dir,
            maxChainedSessions = 3,
            maxActionsPerSession = 5,
            runSession = { _ ->
                dir.resolve("BACKLOG.adoc").writeText(
                    "== EPIC SVO 🟡 EN COURS\n* [ ] SVO-5 chain-up\n* [x] SVO-4 done\n* [x] SVO-3 done"
                )
                "done"
            },
            auditAppend = {},
        )
        assertEquals(3, report.sessionsExecuted, "D9: backlog still open each time → chain up to the bound")
        assertTrue(
            report.resumptionPrompt.contains("SVO-5 chain-up"),
            "resumption prompt orients on the remaining backlog",
        )
        assertTrue(report.resumptionPrompt.contains("work SVO-4"), "mission intention anchors the prompt")
    }

    @Test
    fun `report carries the artifacts written`() {
        val dir = tempDir()
        val report = AutonomousSessionOrchestrator.orchestrate(
            borough = "codebase",
            prompt = "x",
            projectDir = dir,
            maxChainedSessions = 1,
            maxActionsPerSession = 5,
            runSession = { _ -> "done" },
            auditAppend = {},
        )
        assertTrue(report.resumptionPromptPath.isNotBlank(), "resumption prompt path must be reported")
        assertTrue(
            File(report.resumptionPromptPath).exists(),
            "resumption prompt must be physically written under build/vibecoding/",
        )
        assertTrue(
            File(report.resumptionPromptPath).readText().contains("BRAINSTORM"),
            "backlog-less dir → resumption prompt flips to brainstorming",
        )
    }

    @Test
    fun `audit lines are appended per session`() {
        val dir = tempDir()
        val audits = mutableListOf<String>()
        AutonomousSessionOrchestrator.orchestrate(
            borough = "codebase",
            prompt = "x",
            projectDir = dir,
            maxChainedSessions = 2,
            maxActionsPerSession = 5,
            runSession = { _ -> "done" },
            auditAppend = { line -> audits += line },
        )
        assertEquals(2, audits.size, "one audit entry per executed session (mission + brainstorming)")
        assertTrue(audits.first().contains("autonomous_session"))
    }
}
