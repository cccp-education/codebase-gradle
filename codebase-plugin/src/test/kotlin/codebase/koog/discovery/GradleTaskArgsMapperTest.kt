package codebase.koog.discovery

import contracts.vibecoding.tools.ExecGradleTool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GradleTaskArgsMapperTest {

    private val schemas = listOf(
        TaskSchema("build", "Compiles the project", "build", "DefaultTask", emptyList()),
        TaskSchema("test", "Runs tests", "verification", "Test", listOf(
            TaskOption("tests", "Filter tests", false, "String")
        )),
        TaskSchema("publish", "Publishes", "publishing", "PublishToMavenRepository", listOf(
            TaskOption("repository", "Target repo", false, "String"),
            TaskOption("dryRun", "Simulate", false, "Boolean")
        ))
    )

    private val mapper = GradleTaskArgsMapper(schemas)

    @Test
    fun `map structured args to gradle command`() {
        val cmd = mapper.buildCommand(
            toolName = "gradle_test",
            arguments = mapOf("tests" to "FooTest")
        )
        assertEquals("test --tests FooTest", cmd)
    }

    @Test
    fun `map without options should produce plain task name`() {
        val cmd = mapper.buildCommand(
            toolName = "gradle_build",
            arguments = emptyMap()
        )
        assertEquals("build", cmd)
    }

    @Test
    fun `map multiple options`() {
        val cmd = mapper.buildCommand(
            toolName = "gradle_publish",
            arguments = mapOf("repository" to "mavenCentral", "dryRun" to "true")
        )
        assertTrue(cmd.startsWith("publish"))
        assertTrue(cmd.contains("--repository mavenCentral"))
        assertTrue(cmd.contains("--dryRun true"))
    }

    @Test
    fun `map should ignore unknown arg keys`() {
        val cmd = mapper.buildCommand(
            toolName = "gradle_test",
            arguments = mapOf("tests" to "FooTest", "unknown" to "value")
        )
        assertEquals("test --tests FooTest", cmd)
    }

    @Test
    fun `map with boolean flag should pass as string`() {
        val cmd = mapper.buildCommand(
            toolName = "gradle_publish",
            arguments = mapOf("dryRun" to "true")
        )
        assertEquals("publish --dryRun true", cmd)
    }

    @Test
    fun `execute structured gradle task via handler`() {
        val mapper = GradleTaskArgsMapper(schemas, System.getProperty("user.dir")!!)
        val result = mapper.execute(
            toolName = "gradle_test",
            arguments = mapOf("tests" to "FakeTestClassName"),
            workspaceRoot = System.getProperty("user.dir")!!
        )
        assertTrue(result.contains("GRADLE EXIT:"), "Should return gradle exit code: $result")
    }
}
