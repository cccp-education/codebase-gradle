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

    @Test
    fun `task report includes governance sections for recognized files`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")
        File(tempDir, "INDEX.adoc").writeText("= Index\n\n== EPIC Y\nEn cours.\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport!!
        assertTrue(report.sectionsAdded.containsKey(GovernanceSection.RULES_ABSOLUES),
            "Should add chunks to RULES_ABSOLUES")
        assertTrue(report.sectionsAdded.containsKey(GovernanceSection.ETAT_EPICS),
            "Should add chunks to ETAT_EPICS")
        assertTrue(report.sectionsTotal[GovernanceSection.RULES_ABSOLUES] ?: 0 > 0,
            "Should report total for RULES_ABSOLUES")
        assertTrue(report.sectionsTotal[GovernanceSection.ETAT_EPICS] ?: 0 > 0,
            "Should report total for ETAT_EPICS")
    }

    @Test
    fun `task report json includes sectionsAdded and sectionsTotal`(@TempDir tempDir: File) {
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
        assertTrue(content.contains("RULES_ABSOLUES"), "JSON should contain RULES_ABSOLUES")
        assertTrue(content.contains("sectionsAdded"), "JSON should contain sectionsAdded")
        assertTrue(content.contains("sectionsTotal"), "JSON should contain sectionsTotal")
    }

    @Test
    fun `task report json exposes typed validation errors`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val outputFile = File(tempDir, "ingestion-report.json")
        val fakeValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                val base = super.validate(chunk)
                if (base.valid) {
                    val error = ChunkValidationError(
                        sourceFile = chunk.sourceFile,
                        sourceLines = chunk.sourceLines,
                        lineStart = 1,
                        lineEnd = 1,
                        errorType = ChunkValidationErrorType.MISSING_CONTENT,
                        message = "injected validation error"
                    )
                    return ValidationResult(valid = false, errors = listOf(error))
                }
                return base
            }
        }
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.outputFile.set(outputFile)
            it.chunkValidator = fakeValidator
        }.get()

        task.executeIngest()

        assertTrue(outputFile.exists(), "Report file should be written")
        val content = outputFile.readText()
        assertTrue(content.contains("\"errorType\": \"MISSING_CONTENT\""), "JSON should expose errorType")
        assertTrue(content.contains("\"lineStart\": 1"), "JSON should expose lineStart")
        assertTrue(content.contains("\"lineEnd\": 1"), "JSON should expose lineEnd")
        assertTrue(content.contains("\"validationErrorsByType\""), "JSON should expose validationErrorsByType")
        val summaryRegex = Regex("\"validationErrorsByType\"\\s*:\\s*\\{[^}]*\"MISSING_CONTENT\"\\s*:\\s*\\d+")
        assertTrue(summaryRegex.containsMatchIn(content), "JSON should summarize errors by type")
    }
}