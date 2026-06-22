package codebase.koog

import codebase.koog.llm.FakeLlmProvider
import contracts.session.SessionStatus
import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class SessionProtocolTaskTest {

    @Test
    fun `task should be registered with correct group and description`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java).get()

        assertEquals("sessionProtocol", task.name)
        assertEquals("generate", task.group)
        assertTrue(task.description?.contains("Session protocol") == true)
    }

    @Test
    fun `task should have default property values`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java).get()

        assertEquals("", task.prompt.get())
        assertEquals("", task.sessionId.get())
        assertEquals(10, task.maxActions.get())
        assertEquals("", task.model.get())
    }

    @Test
    fun `task should accept custom property values`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Fix typo in README")
            it.sessionId.set("550e8400-e29b-41d4-a716-446655440000")
            it.maxActions.set(5)
            it.model.set("deepseek-v4-pro")
        }.get()

        assertEquals("Fix typo in README", task.prompt.get())
        assertEquals("550e8400-e29b-41d4-a716-446655440000", task.sessionId.get())
        assertEquals(5, task.maxActions.get())
        assertEquals("deepseek-v4-pro", task.model.get())
    }

    @Test
    fun `task should reject blank prompt`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-blank-prompt")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            task.executeProtocol()
        }
        assertTrue(exception.message!!.contains("prompt cannot be blank"))
    }

    @Test
    fun `executeProtocol with FakeLlmProvider produces SessionResponse`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-session-protocol")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Add dark mode toggle")
            it.maxActions.set(3)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()

        val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
        val responseFile = outputDir.resolve("session-response.json")
        assertTrue(responseFile.exists(), "Response file should exist: ${responseFile.absolutePath}")

        val content = responseFile.readText()
        assertTrue(content.contains("sessionId"))
        assertTrue(content.contains("output"))
        assertTrue(content.contains("Add dark mode toggle"))
        assertTrue(content.contains("COMPLETED") || content.contains("IN_PROGRESS"))
    }

    @Test
    fun `executeProtocol with custom sessionId preserves it in response`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-custom-session-id")
            .build()
        project.pluginManager.apply("java-base")

        val customId = "550e8400-e29b-41d4-a716-446655440000"
        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Test with custom ID")
            it.sessionId.set(customId)
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()

        val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
        val responseFile = outputDir.resolve("session-response.json")
        assertTrue(responseFile.exists())
        val content = responseFile.readText()
        assertTrue(content.contains(customId))
    }

    @Test
    fun `executeProtocol with custom responseFile path`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-custom-response-file")
            .build()
        project.pluginManager.apply("java-base")

        val customOutput = tempDir.resolve("my-response.json").toFile()
        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Custom output path")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.responseFile.set(customOutput)
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()

        assertTrue(customOutput.exists(), "Custom response file should exist: ${customOutput.absolutePath}")
        val content = customOutput.readText()
        assertTrue(content.contains("Custom output path"))
    }

    @Test
    fun `executeProtocol with model tracks token usage`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-model-tracking")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Track my tokens")
            it.model.set("deepseek-v4-pro")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()

        val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
        val responseFile = outputDir.resolve("session-response.json")
        assertTrue(responseFile.exists())
        val content = responseFile.readText()
        assertTrue(content.contains("tokenUsage"))
        assertTrue(content.contains("promptTokens"))
        assertTrue(content.contains("completionTokens"))
    }

    @Test
    fun `executeProtocol without contextFile auto-loads governance context`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-governance-fallback")
            .build()
        project.pluginManager.apply("java-base")

        File(tempDir.toFile(), "AGENT.adoc").writeText("= Test Agent Rules\n\nRule 42: governance loaded\n")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Governance fallback test")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()

        val agentContext = task.lastAgentContext
        assertNotNull(agentContext, "AgentContext should be auto-loaded")
        assertTrue(agentContext!!.eagerRules.contains("Rule 42: governance loaded"),
            "Governance context should contain AGENT.adoc rules")

        val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
        val responseFile = outputDir.resolve("session-response.json")
        assertTrue(responseFile.exists())
    }

    @Test
    fun `executeProtocol without contextFile and without governance files continues`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-no-governance")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("No governance test")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()

        val agentContext = task.lastAgentContext
        assertNotNull(agentContext, "AgentContext should be set even when empty")
        assertEquals("", agentContext!!.eagerRules)
        assertEquals(emptyList<String>(), agentContext.backlogItems)

        val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
        val responseFile = outputDir.resolve("session-response.json")
        assertTrue(responseFile.exists())
        assertTrue(responseFile.readText().contains("COMPLETED"))
    }

    @Test
    fun `executeProtocol with contextFile parses AgentContext`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-context-file")
            .build()
        project.pluginManager.apply("java-base")

        val contextFile = tempDir.resolve("agent-context.json").toFile()
        contextFile.writeText("""
            {
                "eagerRules": "Rule 1: No commits without permission",
                "ragChunks": ["chunk1", "chunk2"],
                "graphRelations": "codebase→planner",
                "backlogItems": ["SP-1", "SP-2"]
            }
        """.trimIndent())

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Context-aware prompt")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.contextFile.set(contextFile)
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()

        val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
        val responseFile = outputDir.resolve("session-response.json")
        assertTrue(responseFile.exists())
    }

    @Test
    fun `executeProtocol with corrupt contextFile falls back gracefully`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-corrupt-context")
            .build()
        project.pluginManager.apply("java-base")

        val contextFile = tempDir.resolve("bad-context.json").toFile()
        contextFile.writeText("{ not valid json at all }")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Corrupt context test")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.contextFile.set(contextFile)
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()

        task.executeProtocol()

        val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
        val responseFile = outputDir.resolve("session-response.json")
        assertTrue(responseFile.exists())
    }

    @Test
    fun `executeProtocol with error state returns ERROR status`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-error-status")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("This will fail")
            it.maxActions.set(1)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        val throwingProvider = object : codebase.koog.llm.LlmProvider {
            override suspend fun call(prompt: String): String {
                throw RuntimeException("Simulated LLM failure")
            }
        }
        task.llmProvider = throwingProvider
        task.toolRegistry = ToolRegistry()

        val exception = assertThrows(RuntimeException::class.java) {
            task.executeProtocol()
        }
        assertTrue(exception.message!!.contains("Session protocol failed"))

        val outputDir = project.layout.buildDirectory.dir("session-protocol").get().asFile
        val responseFile = outputDir.resolve("session-response.json")
        assertTrue(responseFile.exists())
        val content = responseFile.readText()
        assertTrue(content.contains("ERROR"))
    }

    @Test
    fun `lifecycle create with action=create persists session`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-lifecycle-create")
            .build()
        project.pluginManager.apply("java-base")

        val lifecycleDir = tempDir.resolve("lifecycle-data").toFile()
        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Create lifecycle test")
            it.action.set("create")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()
        task.lifecycleManager = SessionProtocolLifecycleManager(lifecycleDir)

        task.executeProtocol()

        val sessions = task.lifecycleManager!!.list()
        assertEquals(1, sessions.size)
        assertEquals("Create lifecycle test", sessions[0].prompt)
        assertEquals(LifecycleStatus.RUNNING, sessions[0].status)
        assertNotNull(sessions[0].lastResponseJson)
    }

    @Test
    fun `lifecycle resume creates child session with parent reference`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-lifecycle-resume")
            .build()
        project.pluginManager.apply("java-base")

        val lifecycleDir = tempDir.resolve("lifecycle-data").toFile()
        val lifecycleMgr = SessionProtocolLifecycleManager(lifecycleDir)
        val parent = lifecycleMgr.create("Parent prompt", "gemini")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("Resumed prompt")
            it.sessionId.set(parent.sessionId)
            it.action.set("resume")
            it.maxActions.set(2)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.llmProvider = FakeLlmProvider()
        task.toolRegistry = ToolRegistry()
        task.lifecycleManager = lifecycleMgr

        task.executeProtocol()

        val sessions = lifecycleMgr.list()
        assertTrue(sessions.size >= 2)
        val child = sessions.find { it.parentSessionId == parent.sessionId }
        assertNotNull(child)
        assertEquals("Parent prompt", child?.prompt)
        assertEquals(LifecycleStatus.RUNNING, child?.status)
    }

    @Test
    fun `lifecycle close marks session as CLOSED`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-lifecycle-close")
            .build()
        project.pluginManager.apply("java-base")

        val lifecycleDir = tempDir.resolve("lifecycle-data").toFile()
        val lifecycleMgr = SessionProtocolLifecycleManager(lifecycleDir)
        val created = lifecycleMgr.create("Close me", "model-x")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.sessionId.set(created.sessionId)
            it.action.set("close")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        task.lifecycleManager = lifecycleMgr

        task.executeProtocol()

        val closed = lifecycleMgr.get(created.sessionId)
        assertNotNull(closed)
        assertEquals(LifecycleStatus.CLOSED, closed?.status)
    }

    @Test
    fun `lifecycle list outputs all sessions`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-lifecycle-list")
            .build()
        project.pluginManager.apply("java-base")

        val lifecycleDir = tempDir.resolve("lifecycle-data").toFile()
        val lifecycleMgr = SessionProtocolLifecycleManager(lifecycleDir)
        lifecycleMgr.create("Session A", "model-a")
        lifecycleMgr.create("Session B", "model-b")

        val customOutput = tempDir.resolve("list-output.json").toFile()
        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.action.set("list")
            it.responseFile.set(customOutput)
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        task.lifecycleManager = lifecycleMgr

        task.executeProtocol()

        assertTrue(customOutput.exists())
        val content = customOutput.readText()
        assertTrue(content.contains("Session A"))
        assertTrue(content.contains("Session B"))
        assertTrue(content.contains("COMPLETED"))
    }

    @Test
    fun `lifecycle invalid action throws`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-lifecycle-invalid")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.action.set("invalid")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            task.executeProtocol()
        }
        assertTrue(exception.message!!.contains("Unknown action"))
    }

    @Test
    fun `lifecycle resume with missing sessionId throws`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-lifecycle-missing-session")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("sessionProtocol", SessionProtocolTask::class.java) {
            it.prompt.set("No session ID")
            it.action.set("resume")
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            task.executeProtocol()
        }
        assertTrue(exception.message!!.contains("sessionId"))
    }
}
