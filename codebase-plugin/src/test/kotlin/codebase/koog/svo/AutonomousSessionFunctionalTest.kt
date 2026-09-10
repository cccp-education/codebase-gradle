package codebase.koog.svo

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AutonomousSessionFunctionalTest {

    @Test
    fun `plugin registers the autonomousSession task in generate group`() {
        val project = org.gradle.testfixtures.ProjectBuilder.builder().build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val task = project.tasks.findByName("autonomousSession")
        assertNotNull(task, "Task 'autonomousSession' should be registered")
        assertEquals("generate", task.group)
    }

    @Test
    fun `task options carry the D6 D9 CLI surface`(@TempDir tempDir: Path) {
        val project = org.gradle.testfixtures.ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val task = project.tasks.findByName("autonomousSession") as AutonomousSessionTask
        assertEquals(3, task.maxChainedSessions.get(), "D9 default is 3")
        assertEquals("", task.prompt.get())
        assertEquals("", task.borough.get())
        assertEquals(10, task.maxActions.get())
    }

    @Test
    fun `autonomousSession executes end-to-end on a backlog-less dir — resumption prompt brainstorming`(@TempDir tempDir: Path) {
        val project = org.gradle.testfixtures.ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val task = project.tasks.findByName("autonomousSession") as AutonomousSessionTask
        task.prompt.set("functional mission prompt")
        task.maxChainedSessions.set(3)
        task.maxActions.set(2)

        task.executeAutonomousSession()

        val resumptionFile = tempDir.resolve("build/vibecoding/resumption-prompt.adoc").toFile()
        assertEquals(true, resumptionFile.exists(), "resumption prompt must be written under build/vibecoding/")
        val content = resumptionFile.readText()
        assertEquals(true, content.contains("BRAINSTORM"), "backlog-less dir → brainstorming-oriented prompt")

        val auditFile = tempDir.resolve("build/vibecoding/audit.jsonl").toFile()
        assertEquals(true, auditFile.exists(), "audit JSONL must be appended")
        assertEquals(true, auditFile.readText().contains("autonomous_session"), "audit entry per executed session")
    }

    @Test
    fun `autonomousSession consumes a real BACKLOG — bounded chain`(@TempDir tempDir: Path) {
        val dir = tempDir.toFile()
        var counter = 0
        dir.resolve("BACKLOG.adoc").writeText("== EPIC Test 🟡 EN COURS\n* [ ] endless item")

        val project = org.gradle.testfixtures.ProjectBuilder.builder().withProjectDir(dir).build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val task = project.tasks.findByName("autonomousSession") as AutonomousSessionTask
        task.maxChainedSessions.set(2)
        task.maxActions.set(1)
        task.prompt.set("")

        // The loop re-writes a fresh open item each session — the D9 guard must bound at 2.
        val report = AutonomousSessionOrchestrator.orchestrate(
            borough = "functional-test",
            prompt = null,
            projectDir = dir,
            maxChainedSessions = 2,
            maxActionsPerSession = 2,
            runSession = { _ ->
                counter++
                dir.resolve("BACKLOG.adoc").writeText("== EPIC Loop 🟡 EN COURS\n* [ ] endless item $counter")
                "done $counter"
            },
            auditAppend = { line -> task.let { dir.resolve("build/vibecoding").mkdirs(); java.io.File(dir, "build/vibecoding/audit.jsonl").appendText(line) } },
        )

        assertEquals(2, report.sessionsExecuted, "D9 bound reached")
        assertEquals(AutonomousSessionOrchestrator.STOP_MAX_SESSIONS, report.stopReason)
    }
}
