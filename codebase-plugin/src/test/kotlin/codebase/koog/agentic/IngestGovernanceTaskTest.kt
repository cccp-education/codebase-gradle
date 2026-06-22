package codebase.koog.agentic

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IngestGovernanceTaskTest {

    @Test
    fun `task should be registered in generate group`(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java).get()

        assertEquals("ingestGovernance", task.name)
        assertEquals("generate", task.group)
        assertTrue(task.description?.contains("governance") == true)
    }

    @Test
    fun `task ingests AGENT adoc from workspace root`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText(
            """
            = Agent

            * NE DOIT JAMAIS committer sans permission
            * DOIT valider les tests avant fin de session
            """.trimIndent()
        )

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport
        assertTrue(report != null, "IngestionReport should be set")
        assertTrue(report!!.filesScanned >= 1, "Should scan at least AGENT.adoc, got ${report.filesScanned}")
        assertTrue(report.chunksAdded > 0, "Should add chunks, got ${report.chunksAdded}")
    }

    @Test
    fun `task ingests governance files from subproject`(@TempDir tempDir: File) {
        val subproject = File(tempDir, "codebase-plugin").apply { mkdirs() }
        File(subproject, "AGENT.adoc").writeText("= Sub Agent\n\n* [ ] Item backlog\n")
        File(subproject, "BACKLOG.adoc").writeText("= Backlog\n\n* [ ] Open item\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport!!
        assertTrue(report.filesScanned >= 1, "Should scan subproject files, got ${report.filesScanned}")
        assertTrue(report.chunksAdded > 0, "Should add chunks from subproject, got ${report.chunksAdded}")
    }

    @Test
    fun `task produces empty report for empty workspace`(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport!!
        assertEquals(0, report.filesScanned)
        assertEquals(0, report.chunksAdded)
    }

    @Test
    fun `task writes ingestion report to output file`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val outputFile = File(tempDir, "ingestion-report.json")
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.outputFile.set(outputFile)
        }.get()

        task.executeIngest()

        assertTrue(outputFile.exists(), "Report file should be written")
        val content = outputFile.readText()
        assertTrue(content.contains("filesScanned"))
        assertTrue(content.contains("chunksAdded"))
    }
}