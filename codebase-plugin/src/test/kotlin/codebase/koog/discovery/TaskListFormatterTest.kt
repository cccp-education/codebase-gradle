package codebase.koog.discovery

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskListFormatterTest {

    private val schemas = listOf(
        TaskSchema("build", "Compiles the project", "build", "DefaultTask", emptyList()),
        TaskSchema("test", "Runs unit tests", "verification", "Test", listOf(
            TaskOption("tests", "Filter tests by name", false, "String")
        )),
        TaskSchema("publish", "Publishes artifacts", "publishing", "PublishToMavenRepository", listOf(
            TaskOption("repository", "Target repository", false, "String"),
            TaskOption("dryRun", "Simulate without publishing", false, "Boolean")
        )),
        TaskSchema("clean", "Deletes build output", "build", "Delete", emptyList())
    )

    @Test
    fun `format should produce structured output with task name and description`() {
        val result = TaskListFormatter.format(schemas)
        assertTrue(result.contains("gradle_build"), "Should contain task name prefixed")
        assertTrue(result.contains("Compiles the project"), "Should contain description")
        assertTrue(result.contains("gradle_test"), "Should contain test task")
        assertTrue(result.contains("Runs unit tests"), "Should contain test description")
    }

    @Test
    fun `format should include options for tasks that have them`() {
        val result = TaskListFormatter.format(schemas)
        assertTrue(result.contains("--tests"), "Should list option name")
        assertTrue(result.contains("--repository"), "Should list all options")
        assertTrue(result.contains("--dryRun"), "Should list all options")
    }

    @Test
    fun `format should not include options section for tasks without options`() {
        val result = TaskListFormatter.format(schemas)
        val cleanBlock = result.lines().dropWhile { !it.contains("gradle_clean") }.takeWhile { it.isNotBlank() || it.startsWith("---") }
        val cleanText = cleanBlock.joinToString("\n")
        assertTrue(!cleanText.contains("Options:") || cleanText.lines().last() == "Options: none",
            "Tasks without options should say 'none' or have no Options line")
    }

    @Test
    fun `filter by group should return only matching tasks`() {
        val result = TaskListFormatter.format(schemas, group = "build")
        assertEquals(2, result.lines().count { it.startsWith("- gradle_") },
            "Should return build group tasks only")
        assertTrue(result.contains("gradle_build"))
        assertTrue(!result.contains("gradle_test"))
    }

    @Test
    fun `filter by keyword should match task name`() {
        val result = TaskListFormatter.format(schemas, keyword = "build")
        assertTrue(result.contains("gradle_build"))
        assertTrue(!result.contains("gradle_test"))
        assertTrue(!result.contains("gradle_publish"))
    }

    @Test
    fun `filter by keyword should match description`() {
        val result = TaskListFormatter.format(schemas, keyword = "unit")
        assertTrue(result.contains("gradle_test"))
        assertTrue(!result.contains("gradle_build"))
    }

    @Test
    fun `combined group and keyword filter should intersect`() {
        val result = TaskListFormatter.format(schemas, group = "build", keyword = "compile")
        assertTrue(result.contains("gradle_build"), "build matches compile keyword in build group")
        assertTrue(!result.contains("gradle_clean"), "clean doesn't match compile keyword")
    }

    @Test
    fun `empty query should return all tasks`() {
        val result = TaskListFormatter.format(schemas)
        assertEquals(4, result.lines().count { it.startsWith("- gradle_") })
    }

    @Test
    fun `no match should return empty message`() {
        val result = TaskListFormatter.format(schemas, keyword = "nonexistent")
        assertTrue(result.contains("No tasks found"))
    }
}
