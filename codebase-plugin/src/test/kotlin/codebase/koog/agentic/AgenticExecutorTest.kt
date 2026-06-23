package codebase.koog.agentic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AgenticExecutorTest {

    private val compiler = AgenticCompiler()

    private fun buildExecutable(
        content: String,
        chunkType: ChunkType = ChunkType.RULE,
        verb: TaxonomyVerb? = TaxonomyVerb.INTERDIRE
    ): ExecutableArtifact {
        val chunk = AgenticChunk(
            id = "rule-${content.hashCode()}",
            sourceFile = "AGENT.adoc",
            sourceLines = "1-5",
            chunkType = chunkType,
            content = content,
            verb = verb,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-${content.hashCode()}"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        return compiler.compileExecutable(ontologized)!!
    }

    @BeforeEach
    fun setup() {
    }

    @Test
    fun `should allow all calls when no executables are registered`() {
        val executor = AgenticExecutor()

        val result = executor.check("exec_shell", mapOf("command" to "git push origin main"))

        assertTrue(result.allowed)
        assertEquals(null, result.ruleId)
        assertEquals(null, result.reason)
    }

    @Test
    fun `should block forbidden git push via exec_shell`() {
        val executor = AgenticExecutor(
            executables = listOf(buildExecutable("INTERDICTION FORMELLE de git push sans permission."))
        )

        val result = executor.check("exec_shell", mapOf("command" to "git push origin main"))

        assertFalse(result.allowed)
        assertNotNull(result.ruleId)
        assertNotNull(result.reason)
    }

    @Test
    fun `should allow git push with dry-run exception`() {
        val executor = AgenticExecutor(
            executables = listOf(buildExecutable("INTERDICTION FORMELLE de git push sauf avec --dry-run."))
        )

        val result = executor.check("exec_shell", mapOf("command" to "git push --dry-run origin main"))

        assertTrue(result.allowed)
    }

    @Test
    fun `should allow unrelated tool calls`() {
        val executor = AgenticExecutor(
            executables = listOf(buildExecutable("INTERDICTION FORMELLE de git push sans permission."))
        )

        assertTrue(executor.check("exec_gradle", mapOf("task" to "build")).allowed)
        assertTrue(executor.check("exec_shell", mapOf("command" to "ls -la")).allowed)
    }

    @Test
    fun `should block gradle publish via exec_gradle`() {
        val executor = AgenticExecutor(
            executables = listOf(buildExecutable("INTERDICTION FORMELLE de ./gradlew publish sans verification."))
        )

        val result = executor.check("exec_gradle", mapOf("task" to "publish"))

        assertFalse(result.allowed)
    }

    @Test
    fun `should allow gradle publish with dry-run exception`() {
        val executor = AgenticExecutor(
            executables = listOf(buildExecutable("INTERDICTION FORMELLE de ./gradlew publish sauf --dry-run."))
        )

        val result = executor.check("exec_gradle", mapOf("task" to "publish --dry-run"))

        assertTrue(result.allowed)
    }

    @Test
    fun `should block first matching rule and return its id`() {
        val ruleA = buildExecutable("INTERDICTION FORMELLE de commit sans permission.")
        val ruleB = buildExecutable("INTERDICTION FORMELLE de merge sans permission.")
        val executor = AgenticExecutor(executables = listOf(ruleA, ruleB))

        val result = executor.check("exec_shell", mapOf("command" to "git commit -m 'test'"))

        assertFalse(result.allowed)
        assertEquals(ruleA.compiledArtifact.sourceChunkId, result.ruleId)
    }

    @Test
    fun `should ignore non hook artifacts`() {
        val metadata = buildExecutable(
            content = ":date: 2026-06-23",
            chunkType = ChunkType.METADATA,
            verb = null
        )
        val preHook = buildExecutable("INTERDICTION FORMELLE de git push sans permission.")
        val executor = AgenticExecutor(executables = listOf(metadata, preHook))

        val result = executor.check("exec_shell", mapOf("command" to "git push origin main"))

        assertFalse(result.allowed)
    }
}
