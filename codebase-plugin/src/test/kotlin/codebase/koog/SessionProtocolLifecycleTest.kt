package codebase.koog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID

class SessionProtocolLifecycleTest {

    @Test
    fun `create starts a new session with CREATED status`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val state = manager.create("Fix typo in README", "deepseek-v4-pro")

        assertEquals("Fix typo in README", state.prompt)
        assertEquals(LifecycleStatus.CREATED, state.status)
        assertNotNull(state.sessionId)
        UUID.fromString(state.sessionId)
        assertNull(state.lastResponseJson)
        assertNull(state.parentSessionId)
    }

    @Test
    fun `resume loads an existing session with RUNNING status`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val created = manager.create("Add dark mode toggle", "deepseek-v4-pro")
        val resumed = manager.resume(created.sessionId)

        assertEquals(created.sessionId, resumed.parentSessionId)
        assertEquals("Add dark mode toggle", resumed.prompt)
        assertEquals(LifecycleStatus.RUNNING, resumed.status)
    }

    @Test
    fun `close marks session as CLOSED`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val created = manager.create("Add tests", "gemini")
        manager.close(created.sessionId)

        val closed = manager.get(created.sessionId)
        assertEquals(LifecycleStatus.CLOSED, closed?.status)
    }

    @Test
    fun `get returns null for non-existent session`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        assertNull(manager.get("nonexistent-id"))
    }

    @Test
    fun `list returns all sessions ordered by created_at desc`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val s1 = manager.create("First prompt", "model-a")
        Thread.sleep(10)
        val s2 = manager.create("Second prompt", "model-b")

        val sessions = manager.list()
        assertEquals(2, sessions.size)
        assertEquals(s2.sessionId, sessions[0].sessionId)
        assertEquals(s1.sessionId, sessions[1].sessionId)
    }

    @Test
    fun `listByStatus filters sessions`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val s1 = manager.create("Prompt A", "model-a")
        manager.close(s1.sessionId)
        manager.create("Prompt B", "model-b")

        val created = manager.listByStatus(LifecycleStatus.CREATED)
        assertEquals(1, created.size)
        assertEquals("Prompt B", created[0].prompt)

        val closed = manager.listByStatus(LifecycleStatus.CLOSED)
        assertEquals(1, closed.size)
        assertEquals("Prompt A", closed[0].prompt)
    }

    @Test
    fun `updateResponse persists execution result`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val created = manager.create("Execute task", "model-x")

        val responseJson = """
            {"sessionId":"${created.sessionId}","output":"Task done","status":"COMPLETED"}
        """.trimIndent()
        manager.updateResponse(created.sessionId, responseJson)

        val updated = manager.get(created.sessionId)
        assertEquals(LifecycleStatus.RUNNING, updated?.status)
        assertEquals(responseJson, updated?.lastResponseJson)
    }

    @Test
    fun `resume stores parentSessionId reference`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val created = manager.create("Original prompt", "model-a")
        val resumed = manager.resume(created.sessionId)

        assertEquals(created.sessionId, resumed.parentSessionId)
        assertEquals("Original prompt", resumed.prompt)
    }

    @Test
    fun `resume non-existent session throws exception`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())

        val exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            manager.resume("nonexistent")
        }
        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `close non-existent session throws exception`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())

        val exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            manager.close("nonexistent")
        }
        assertTrue(exception.message!!.contains("not found"))
    }

    @Test
    fun `delete removes session from storage`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val created = manager.create("Temporary session", "model-x")

        val deleted = manager.delete(created.sessionId)
        assertTrue(deleted)
        assertNull(manager.get(created.sessionId))
    }

    @Test
    fun `delete non-existent session returns false`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        assertFalse(manager.delete("nonexistent"))
    }

    @Test
    fun `persistence survives manager recreation`(@TempDir tempDir: Path) {
        val manager1 = SessionProtocolLifecycleManager(tempDir.toFile())
        val created = manager1.create("Persistent prompt", "model-p")

        val manager2 = SessionProtocolLifecycleManager(tempDir.toFile())
        val loaded = manager2.get(created.sessionId)

        assertNotNull(loaded)
        assertEquals(created.sessionId, loaded?.sessionId)
        assertEquals("Persistent prompt", loaded?.prompt)
        assertEquals(LifecycleStatus.CREATED, loaded?.status)
    }

    @Test
    fun `createdAt and updatedAt are populated`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val state = manager.create("Timestamp test", "model-t")

        assertNotNull(state.createdAt)
        assertNotNull(state.updatedAt)
        assertTrue(state.createdAt == state.updatedAt || state.createdAt.isBefore(state.updatedAt))
    }

    @Test
    fun `model is stored and retrieved`(@TempDir tempDir: Path) {
        val manager = SessionProtocolLifecycleManager(tempDir.toFile())
        val state = manager.create("Model test", "gemini-2.5-flash")

        assertEquals("gemini-2.5-flash", state.model)

        val loaded = manager.get(state.sessionId)
        assertEquals("gemini-2.5-flash", loaded?.model)
    }
}
