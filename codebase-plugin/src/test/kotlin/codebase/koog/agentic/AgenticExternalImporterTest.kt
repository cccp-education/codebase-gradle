package codebase.koog.agentic

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AgenticExternalImporterTest {

    private lateinit var fakeRepo: FakeAgenticChunkRepository
    private lateinit var importer: AgenticExternalImporter

    @BeforeEach
    fun setup() {
        fakeRepo = FakeAgenticChunkRepository()
        importer = AgenticExternalImporter(
            ingestor = AgenticIngestor(repository = fakeRepo)
        )
    }

    @Test
    fun `should import a copilot prompt markdown`() = runBlocking {
        val copilotPrompt = """
            # Copilot Rules

            You are an expert Kotlin developer.
            Always write unit tests before implementation.
            Never commit secrets to the repository.
            Follow clean architecture principles.

            ## Code Style
            - Use 4 spaces for indentation
            - Max line length: 120 characters
            - Use meaningful variable names
        """.trimIndent()

        val report = importer.import("copilot", "copilot-rules", copilotPrompt)

        assertTrue(report.chunksAdded > 0, "Should add chunks from copilot prompt")
        assertTrue(report.artifactsCompiled > 0, "Should compile artifacts")
        assertEquals(1, report.filesScanned, "Should scan one pseudo-file")
        assertTrue(fakeRepo.countChunks() > 0, "Repo should have chunks")
    }

    @Test
    fun `should import cursor rules in yaml frontmatter format`() = runBlocking {
        val cursorRules = """
            ---
            name: Strict Kotlin Style
            version: 1.0
            rules:
              - Never use !! operator
              - Always handle nullable types safely
              - Prefer data classes for models
              - Max function length: 30 lines
            ---

            # Cursor Instructions

            When generating Kotlin code:
            1. Always add null safety checks
            2. Use extension functions judiciously
            3. Avoid mutable state in data classes
        """.trimIndent()

        val report = importer.import("cursor", "cursor-rules", cursorRules)

        assertTrue(report.chunksAdded > 0, "Should add chunks from cursor rules")
        assertTrue(report.artifactsCompiled > 0, "Should compile artifacts")
        assertEquals(1, report.filesScanned)
    }

    @Test
    fun `should import claude agent rules`() = runBlocking {
        val claudeRules = """
            System Prompt for Claude Agent:

            You are a senior software engineer working on a Gradle plugin ecosystem.
            Your responsibilities:
            - Write clean, idiomatic Kotlin code
            - Follow TDD methodology strictly
            - Never modify user's personal configuration files
            - Always verify compilation after each change
            - Use 5x20min sessions over 1x2h marathon sessions

            Constraints:
            - Maximum context window: 200k tokens
            - No external API calls without user permission
            - All generated code must compile on first attempt
        """.trimIndent()

        val report = importer.import("claude", "claude-agent", claudeRules)

        assertTrue(report.chunksAdded > 0, "Should add chunks from claude rules")
        assertTrue(report.artifactsCompiled > 0, "Should compile artifacts")
        assertEquals(1, report.filesScanned)
    }

    @Test
    fun `should import gemini system prompt`() = runBlocking {
        val geminiPrompt = """
            # System Instruction

            You are Gemini, a code generation assistant specialized in Gradle plugins.

            ## Coding Standards
            - Kotlin 2.x with coroutines
            - Gradle 8.x build scripts
            - JUnit 5 for testing
            - Cucumber for BDD

            ## Prohibitions
            - Do not generate code with hardcoded secrets
            - Do not use deprecated APIs
            - Do not generate code without corresponding tests

            ## Quality Gates
            - All code must pass `./gradlew check`
            - Test coverage must be above 80%
        """.trimIndent()

        val report = importer.import("gemini", "gemini-system", geminiPrompt)

        assertTrue(report.chunksAdded > 0, "Should add chunks from gemini prompt")
        assertTrue(report.artifactsCompiled > 0, "Should compile artifacts")
        assertEquals(1, report.filesScanned)
    }

    @Test
    fun `should handle empty external content`() = runBlocking {
        val report = importer.import("copilot", "empty-prompt", "   \n  \n   ")

        assertEquals(0, report.chunksAdded)
        assertEquals(0, report.chunksSkipped)
        assertEquals(0, report.chunksModified)
        assertEquals(0, report.artifactsCompiled)
    }

    @Test
    fun `should normalize external format before delegating to ingestor`() = runBlocking {
        val rawExternal = """
            ## Rule: Never commit secrets
            INTERDICTION de commit de tokens API.
            
            ## Procedure: Build workflow
            1. Write test
            2. Implement code
            3. Verify compilation
            4. Verify tests pass
        """.trimIndent()

        val report = importer.import("copilot", "some-rules", rawExternal)

        assertTrue(report.chunksAdded > 0)
        val chunks = fakeRepo.listChunks(Int.MAX_VALUE)
        val ruleChunks = chunks.filter { it.chunk.chunkType == ChunkType.RULE }
        assertTrue(ruleChunks.isNotEmpty(), "Should have at least one RULE chunk from INTERDICTION pattern")
        val sourceFiles = chunks.map { it.chunk.sourceFile }.toSet()
        assertTrue(sourceFiles.all { it.startsWith("copilot:") },
            "All chunks should have copilot: source prefix, got: $sourceFiles")
    }

    @Test
    fun `should set correct source file prefix in chunked content`() = runBlocking {
        val copilotRules = """
            # Copilot Rule
            Always write tests first.
            INTERDICTION de code sans tests.
        """.trimIndent()

        val report = importer.import("copilot", "tdd-rules", copilotRules)

        assertTrue(report.chunksAdded > 0)
        val chunks = fakeRepo.listChunks(Int.MAX_VALUE)
        val sourceFiles = chunks.map { it.chunk.sourceFile }.toSet()
        assertTrue(sourceFiles.all { it.startsWith("copilot:") },
            "All chunks should have 'copilot:' prefix in sourceFile, got: $sourceFiles")
    }
}
