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

    // ── US-4 C1 : expectedOutput comparison + refined "failed" heuristic ──

    @Test
    fun `zero failed tests should return SUCCESS not FAILED`() {
        val result = verifier.verify("BUILD SUCCESSFUL\n0 failed tests, 10 passed", "")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }

    @Test
    fun `zero failed with no build successful should not be FAILED`() {
        val result = verifier.verify("Test run: 0 failed, 5 passed", "")
        assertEquals(TaskVerdict.UNKNOWN, result.verdict)
    }

    @Test
    fun `N tests failed still returns FAILED`() {
        val result = verifier.verify("BUILD FAILED\n3 tests failed", "assertion error")
        assertEquals(TaskVerdict.FAILED, result.verdict)
    }

    @Test
    fun `custom expectedOutput matching stdout should return SUCCESS`() {
        val result = verifier.verify("SPG generated successfully at /tmp/spg.adoc", "", "SPG generated")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }

    @Test
    fun `custom expectedOutput not matching stdout should return FAILED`() {
        val result = verifier.verify("Some random output", "", "SPG generated")
        assertEquals(TaskVerdict.FAILED, result.verdict)
    }

    @Test
    fun `custom expectedOutput with BUILD FAILED should return FAILED`() {
        val result = verifier.verify("SPG generated", "BUILD FAILED in 2s", "SPG generated")
        assertEquals(TaskVerdict.FAILED, result.verdict)
    }

    @Test
    fun `custom expectedOutput matching with zero failed tests should return SUCCESS`() {
        val result = verifier.verify("SPG generated\n0 failed tests", "", "SPG generated")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }

    @Test
    fun `custom expectedOutput case insensitive matching should return SUCCESS`() {
        val result = verifier.verify("SPG GENERATED Successfully", "", "spg generated")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }

    @Test
    fun `default expectedOutput with zero failed tests returns SUCCESS`() {
        val result = verifier.verify("BUILD SUCCESSFUL\n0 failed tests", "", "BUILD SUCCESSFUL")
        assertEquals(TaskVerdict.SUCCESS, result.verdict)
    }

    @Test
    fun `custom expectedOutput absent with zero failed tests returns FAILED`() {
        val result = verifier.verify("0 failed tests\n5 passed", "", "SPG generated")
        assertEquals(TaskVerdict.FAILED, result.verdict)
    }
}
