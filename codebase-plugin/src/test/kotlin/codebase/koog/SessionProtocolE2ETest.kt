package codebase.koog

import codebase.koog.llm.FakeLlmProvider
import contracts.session.SessionStatus
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SessionProtocolE2ETest {

    @Test
    fun `E2E create session vibecode get response close session`(@TempDir tempDir: Path) {
        val lifecycleDir = tempDir.resolve("e2e-lifecycle").toFile()
        val lifecycleMgr = SessionProtocolLifecycleManager(lifecycleDir)

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("e2e-test")
            .build()
        project.pluginManager.apply("java-base")

        val createTask = project.tasks.register("sessionProtocolCreate", SessionProtocolTask::class.java) {
            it.prompt.set("Add dark mode toggle to settings")
            it.action.set("create")
            it.maxActions.set(3)
            it.model.set("gemma4:31b-cloud")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        createTask.llmProvider = FakeLlmProvider()
        createTask.toolRegistry = ToolRegistry()
        createTask.lifecycleManager = lifecycleMgr

        createTask.executeProtocol()

        val sessions = lifecycleMgr.list()
        assertEquals(1, sessions.size)
        val created = lifecycleMgr.list()[0]
        assertEquals(LifecycleStatus.RUNNING, created.status)
        assertEquals("Add dark mode toggle to settings", created.prompt)
        assertEquals("gemma4:31b-cloud", created.model)
        assertNotNull(created.lastResponseJson)

        val responseJson = created.lastResponseJson!!
        assertTrue(responseJson.contains("sessionId"))
        assertTrue(responseJson.contains("COMPLETED"))
        assertTrue(responseJson.contains("Add dark mode toggle to settings"))
        assertTrue(responseJson.contains("tokenUsage"))
        assertTrue(responseJson.contains("promptTokens"))
        assertTrue(responseJson.contains("completionTokens"))

        val closeTask = project.tasks.register("sessionProtocolClose", SessionProtocolTask::class.java) {
            it.sessionId.set(created.sessionId)
            it.action.set("close")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        closeTask.lifecycleManager = lifecycleMgr

        closeTask.executeProtocol()

        val closed = lifecycleMgr.get(created.sessionId)
        assertNotNull(closed)
        assertEquals(LifecycleStatus.CLOSED, closed!!.status)
    }

    @Test
    fun `E2E create with custom sessionId vibecode close`(@TempDir tempDir: Path) {
        val lifecycleDir = tempDir.resolve("e2e-custom-id").toFile()
        val lifecycleMgr = SessionProtocolLifecycleManager(lifecycleDir)
        val customId = "550e8400-e29b-41d4-a716-446655440000"

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("e2e-custom-id")
            .build()
        project.pluginManager.apply("java-base")

        val createTask = project.tasks.register("sessionProtocolCreate", SessionProtocolTask::class.java) {
            it.prompt.set("Custom ID E2E test")
            it.sessionId.set(customId)
            it.action.set("create")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        createTask.llmProvider = FakeLlmProvider()
        createTask.toolRegistry = ToolRegistry()
        createTask.lifecycleManager = lifecycleMgr

        createTask.executeProtocol()

        val session = lifecycleMgr.get(customId)
        assertNotNull(session)
        assertEquals(customId, session!!.sessionId)
        assertEquals("Custom ID E2E test", session.prompt)
        assertTrue(session.lastResponseJson!!.contains(customId))

        val closeTask = project.tasks.register("sessionProtocolClose", SessionProtocolTask::class.java) {
            it.sessionId.set(customId)
            it.action.set("close")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        closeTask.lifecycleManager = lifecycleMgr

        closeTask.executeProtocol()

        assertEquals(LifecycleStatus.CLOSED, lifecycleMgr.get(customId)!!.status)
    }

    @Test
    fun `E2E create resume child close parent`(@TempDir tempDir: Path) {
        val lifecycleDir = tempDir.resolve("e2e-resume").toFile()
        val lifecycleMgr = SessionProtocolLifecycleManager(lifecycleDir)

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("e2e-resume")
            .build()
        project.pluginManager.apply("java-base")

        val createTask = project.tasks.register("sessionProtocolCreate", SessionProtocolTask::class.java) {
            it.prompt.set("Initial work session")
            it.action.set("create")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        createTask.llmProvider = FakeLlmProvider()
        createTask.toolRegistry = ToolRegistry()
        createTask.lifecycleManager = lifecycleMgr

        createTask.executeProtocol()

        val parent = lifecycleMgr.list()[0]
        assertEquals(LifecycleStatus.RUNNING, parent.status)

        val resumeTask = project.tasks.register("sessionProtocolResume", SessionProtocolTask::class.java) {
            it.prompt.set("Continue previous work")
            it.sessionId.set(parent.sessionId)
            it.action.set("resume")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        resumeTask.llmProvider = FakeLlmProvider()
        resumeTask.toolRegistry = ToolRegistry()
        resumeTask.lifecycleManager = lifecycleMgr

        resumeTask.executeProtocol()

        val allSessions = lifecycleMgr.list()
        assertTrue(allSessions.size >= 2)
        val child = allSessions.find { it.parentSessionId == parent.sessionId }
        assertNotNull(child)
        assertEquals(LifecycleStatus.RUNNING, child!!.status)
        assertNotNull(child.lastResponseJson)

        val closeTask = project.tasks.register("sessionProtocolClose", SessionProtocolTask::class.java) {
            it.sessionId.set(parent.sessionId)
            it.action.set("close")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        closeTask.lifecycleManager = lifecycleMgr

        closeTask.executeProtocol()

        assertEquals(LifecycleStatus.CLOSED, lifecycleMgr.get(parent.sessionId)!!.status)
    }

    @Test
    fun `E2E create with contextFile vibecode close`(@TempDir tempDir: Path) {
        val lifecycleDir = tempDir.resolve("e2e-context").toFile()
        val lifecycleMgr = SessionProtocolLifecycleManager(lifecycleDir)

        val contextFile = tempDir.resolve("agent-context.json").toFile()
        contextFile.writeText("""
            {
                "eagerRules": "Rule 1: No commits without permission",
                "ragChunks": ["chunk-a", "chunk-b"],
                "graphRelations": "codebase→planner→training",
                "backlogItems": ["SP-1", "SP-2", "SP-3"]
            }
        """.trimIndent())

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("e2e-context")
            .build()
        project.pluginManager.apply("java-base")

        val createTask = project.tasks.register("sessionProtocolCreate", SessionProtocolTask::class.java) {
            it.prompt.set("Context-aware E2E task")
            it.action.set("create")
            it.maxActions.set(3)
            it.contextFile.set(contextFile)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        createTask.llmProvider = FakeLlmProvider()
        createTask.toolRegistry = ToolRegistry()
        createTask.lifecycleManager = lifecycleMgr

        createTask.executeProtocol()

        val sessions = lifecycleMgr.list()
        assertEquals(1, sessions.size)
        assertTrue(sessions[0].lastResponseJson!!.contains("COMPLETED"))

        val closeTask = project.tasks.register("sessionProtocolClose", SessionProtocolTask::class.java) {
            it.sessionId.set(sessions[0].sessionId)
            it.action.set("close")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        closeTask.lifecycleManager = lifecycleMgr

        closeTask.executeProtocol()

        assertEquals(LifecycleStatus.CLOSED, lifecycleMgr.get(sessions[0].sessionId)!!.status)
    }

    @Test
    fun `E2E create list close verify list reflects closure`(@TempDir tempDir: Path) {
        val lifecycleDir = tempDir.resolve("e2e-list-close").toFile()
        val lifecycleMgr = SessionProtocolLifecycleManager(lifecycleDir)

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("e2e-list-close")
            .build()
        project.pluginManager.apply("java-base")

        val createTask = project.tasks.register("sessionProtocolCreate", SessionProtocolTask::class.java) {
            it.prompt.set("Session Alpha")
            it.action.set("create")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        createTask.llmProvider = FakeLlmProvider()
        createTask.toolRegistry = ToolRegistry()
        createTask.lifecycleManager = lifecycleMgr

        createTask.executeProtocol()

        val listOutput = tempDir.resolve("list-output.json").toFile()
        val listTask = project.tasks.register("sessionProtocolList", SessionProtocolTask::class.java) {
            it.action.set("list")
            it.responseFile.set(listOutput)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        listTask.lifecycleManager = lifecycleMgr

        listTask.executeProtocol()

        val listContent = listOutput.readText()
        assertTrue(listContent.contains("Session Alpha"))
        assertTrue(listContent.contains("RUNNING"))

        val sessionId = lifecycleMgr.list()[0].sessionId
        val closeTask = project.tasks.register("sessionProtocolClose", SessionProtocolTask::class.java) {
            it.sessionId.set(sessionId)
            it.action.set("close")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        closeTask.lifecycleManager = lifecycleMgr

        closeTask.executeProtocol()

        val listAfterClose = tempDir.resolve("list-after-close.json").toFile()
        val listTask2 = project.tasks.register("sessionProtocolList2", SessionProtocolTask::class.java) {
            it.action.set("list")
            it.responseFile.set(listAfterClose)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        listTask2.lifecycleManager = lifecycleMgr

        listTask2.executeProtocol()

        val listAfterContent = listAfterClose.readText()
        assertTrue(listAfterContent.contains("Session Alpha"))
        assertTrue(listAfterContent.contains("CLOSED"))
    }
}
