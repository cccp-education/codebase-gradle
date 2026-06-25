package codebase.koog.planning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskResultVerifierTest {

    private val verifier = TaskResultVerifier()

    @Test
    fun `BUILD SUCCESSFUL should return SUCCESS`() {
        val result = verifier.verify("BUILD SUCCESSFUL in 5s", "")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }

    @Test
    fun `BUILD FAILED should return FAILED`() {
        val result = verifier.verify("BUILD FAILED in 2s", "error: compilation error")
        assertEquals(TaskVerdict.FAILED, result.verdict)
    }

    @Test
    fun `test task with all tests passed should return SUCCESS`() {
        val result = verifier.verify("BUILD SUCCESSFUL\nAll tests passed", "")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }

    @Test
    fun `test task with failures should return FAILED`() {
        val result = verifier.verify("BUILD FAILED\n3 tests failed", "FooTest: assertion error")
        assertEquals(TaskVerdict.FAILED, result.verdict)
    }

    @Test
    fun `missing dependency should return BLOCKED`() {
        val result = verifier.verify("", "Task 'generateSPD' not found in project")
        assertEquals(TaskVerdict.BLOCKED, result.verdict)
    }

    @Test
    fun `unknown output should return UNKNOWN`() {
        val result = verifier.verify("Some random output", "")
        assertEquals(TaskVerdict.UNKNOWN, result.verdict)
    }

    @Test
    fun `empty output should return UNKNOWN`() {
        val result = verifier.verify("", "")
        assertEquals(TaskVerdict.UNKNOWN, result.verdict)
    }

    @Test
    fun `SUCCESS result should have no error message`() {
        val result = verifier.verify("BUILD SUCCESSFUL", "")
        assertEquals("", result.errorMessage)
    }

    @Test
    fun `FAILED result should contain error message`() {
        val result = verifier.verify("BUILD FAILED", "compilation error in Foo.kt:42")
        assertEquals(TaskVerdict.FAILED, result.verdict)
        assertEquals("compilation error in Foo.kt:42", result.errorMessage)
    }

    @Test
    fun `BLOCKED result should contain stderr`() {
        val result = verifier.verify("", "Task 'publish' not found")
        assertEquals(TaskVerdict.BLOCKED, result.verdict)
        assertEquals("Task 'publish' not found", result.errorMessage)
    }

    @Test
    fun `FAILED with tests failed pattern should be detected`() {
        val result = verifier.verify("BUILD FAILED\nTests FAILED", "2 tests failed")
        assertEquals(TaskVerdict.FAILED, result.verdict)
    }

    @Test
    fun `FAILED with compilation error pattern should be detected`() {
        val result = verifier.verify("BUILD FAILED", "Compilation error: unresolved reference")
        assertEquals(TaskVerdict.FAILED, result.verdict)
    }

    @Test
    fun `BLOCKED with task not found pattern should be detected`() {
        val result = verifier.verify("", "Task 'nonexistent' not found in root project")
        assertEquals(TaskVerdict.BLOCKED, result.verdict)
    }

    @Test
    fun `BLOCKED with unknown project pattern should be detected`() {
        val result = verifier.verify("", "Project 'missing' not found")
        assertEquals(TaskVerdict.BLOCKED, result.verdict)
    }

    @Test
    fun `DRY RUN output should return SUCCESS`() {
        val result = verifier.verify("DRY RUN: would execute gradle task: tasks", "")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }

    @Test
    fun `DRY RUN write_file output should return SUCCESS`() {
        val result = verifier.verify("DRY RUN: would write 42 chars to /tmp/foo.txt", "")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }
}
