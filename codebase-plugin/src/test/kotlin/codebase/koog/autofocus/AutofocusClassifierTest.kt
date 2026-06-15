package codebase.koog.autofocus

import codebase.koog.llm.LlmProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AutofocusClassifierTest {

    @Test
    fun `classifySync detects compilation error as IMPLEMENTATION`() {
        val result = AutofocusClassifier.classifySync("fix compilation error in VibecodingGraph.kt")
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
    }

    @Test
    fun `classifySync detects build fail as IMPLEMENTATION`() {
        val result = AutofocusClassifier.classifySync("build fail on codebase-plugin")
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
    }

    @Test
    fun `classifySync detects file line reference as IMPLEMENTATION`() {
        val result = AutofocusClassifier.classifySync("error in VibecodingGraph.kt:42")
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
    }

    @Test
    fun `classifySync detects fix bug as IMPLEMENTATION`() {
        val result = AutofocusClassifier.classifySync("fix this bug in TokenTracker")
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
    }

    @Test
    fun `classifySync detects error line pattern as IMPLEMENTATION`() {
        val result = AutofocusClassifier.classifySync("error: unresolved reference line 42")
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
    }

    @Test
    fun `classifySync detects refactor module as MODULE`() {
        val result = AutofocusClassifier.classifySync("refactor the auth module")
        assertEquals(AutofocusLevel.MODULE, result)
    }

    @Test
    fun `classifySync detects extract class as MODULE`() {
        val result = AutofocusClassifier.classifySync("extract UserService class from AuthModule")
        assertEquals(AutofocusLevel.MODULE, result)
    }

    @Test
    fun `classifySync detects implement task as MODULE`() {
        val result = AutofocusClassifier.classifySync("implement the OcrTask for batch processing")
        assertEquals(AutofocusLevel.MODULE, result)
    }

    @Test
    fun `classifySync detects add test as MODULE`() {
        val result = AutofocusClassifier.classifySync("add test for SessionRepository")
        assertEquals(AutofocusLevel.MODULE, result)
    }

    @Test
    fun `classifySync detects gradle file as MODULE`() {
        val result = AutofocusClassifier.classifySync("update build.gradle.kts dependencies")
        assertEquals(AutofocusLevel.MODULE, result)
    }

    @Test
    fun `classifySync detects architecture keyword as ARCHITECTURE`() {
        val result = AutofocusClassifier.classifySync("review the architecture of the workspace")
        assertEquals(AutofocusLevel.ARCHITECTURE, result)
    }

    @Test
    fun `classifySync detects DAG as ARCHITECTURE`() {
        val result = AutofocusClassifier.classifySync("analyze the DAG dependency graph")
        assertEquals(AutofocusLevel.ARCHITECTURE, result)
    }

    @Test
    fun `classifySync detects dependency as ARCHITECTURE`() {
        val result = AutofocusClassifier.classifySync("dépendance entre codebase et codex")
        assertEquals(AutofocusLevel.ARCHITECTURE, result)
    }

    @Test
    fun `classifySync detects contract change as ARCHITECTURE`() {
        val result = AutofocusClassifier.classifySync("API change in the contract layer")
        assertEquals(AutofocusLevel.ARCHITECTURE, result)
    }

    @Test
    fun `classifySync detects MVP as BIG_PICTURE`() {
        val result = AutofocusClassifier.classifySync("what is the status of MVP0?")
        assertEquals(AutofocusLevel.BIG_PICTURE, result)
    }

    @Test
    fun `classifySync detects roadmap as BIG_PICTURE`() {
        val result = AutofocusClassifier.classifySync("update the roadmap for Q3")
        assertEquals(AutofocusLevel.BIG_PICTURE, result)
    }

    @Test
    fun `classifySync detects EPIC as BIG_PICTURE`() {
        val result = AutofocusClassifier.classifySync("what is the status of EPIC Z?")
        assertEquals(AutofocusLevel.BIG_PICTURE, result)
    }

    @Test
    fun `classifySync detects code review as BIG_PICTURE`() {
        val result = AutofocusClassifier.classifySync("run a global code review")
        assertEquals(AutofocusLevel.BIG_PICTURE, result)
    }

    @Test
    fun `classifySync detects session as BIG_PICTURE`() {
        val result = AutofocusClassifier.classifySync("start session 107")
        assertEquals(AutofocusLevel.BIG_PICTURE, result)
    }

    @Test
    fun `classifySync returns null for unknown intention`() {
        val result = AutofocusClassifier.classifySync("hello world")
        assertNull(result)
    }

    @Test
    fun `classifySync returns null for empty intention`() {
        val result = AutofocusClassifier.classifySync("")
        assertNull(result)
    }

    @Test
    fun `classifySync uses buildOutput for classification`() {
        val result = AutofocusClassifier.classifySync(
            intention = "something went wrong",
            buildOutput = "BUILD FAILED in 3s"
        )
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
    }

    @Test
    fun `classifySync buildOutput BUILD FAILED takes priority over roadmap intention`() {
        val result = AutofocusClassifier.classifySync(
            intention = "what is the roadmap for EPIC Z?",
            buildOutput = "BUILD FAILED in 3s"
        )
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
    }

    @Test
    fun `classify with LLM fallback returns MODULE for unknown intention`() = runBlocking {
        val fakeLlm = LlmProvider { "module" }
        val result = AutofocusClassifier.classify(
            intention = "some random task",
            llmProvider = fakeLlm
        )
        assertEquals(AutofocusLevel.MODULE, result)
    }

    @Test
    fun `classify with LLM fallback returns BIG_PICTURE when LLM says big-picture`() = runBlocking {
        val fakeLlm = LlmProvider { "big-picture" }
        val result = AutofocusClassifier.classify(
            intention = "unknown strategic question",
            llmProvider = fakeLlm
        )
        assertEquals(AutofocusLevel.BIG_PICTURE, result)
    }

    @Test
    fun `classify with LLM fallback returns ARCHITECTURE when LLM says architecture`() = runBlocking {
        val fakeLlm = LlmProvider { "architecture" }
        val result = AutofocusClassifier.classify(
            intention = "unknown dependency question",
            llmProvider = fakeLlm
        )
        assertEquals(AutofocusLevel.ARCHITECTURE, result)
    }

    @Test
    fun `classify with LLM fallback returns IMPLEMENTATION when LLM says implementation`() = runBlocking {
        val fakeLlm = LlmProvider { "implementation" }
        val result = AutofocusClassifier.classify(
            intention = "unknown code question",
            llmProvider = fakeLlm
        )
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
    }

    @Test
    fun `classify skips LLM when heuristic matches`() = runBlocking {
        var llmCalled = false
        val fakeLlm = LlmProvider {
            llmCalled = true
            "big-picture"
        }
        val result = AutofocusClassifier.classify(
            intention = "fix compilation error",
            llmProvider = fakeLlm
        )
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
        assertEquals(false, llmCalled)
    }

    @Test
    fun `classify with buildOutput triggers heuristic before LLM`() = runBlocking {
        var llmCalled = false
        val fakeLlm = LlmProvider {
            llmCalled = true
            "module"
        }
        val result = AutofocusClassifier.classify(
            intention = "something",
            buildOutput = "BUILD FAILED",
            llmProvider = fakeLlm
        )
        assertEquals(AutofocusLevel.IMPLEMENTATION, result)
        assertEquals(false, llmCalled)
    }

    @Test
    fun `classifySync is case insensitive`() {
        assertEquals(AutofocusLevel.BIG_PICTURE, AutofocusClassifier.classifySync("MVP0 STATUS"))
        assertEquals(AutofocusLevel.ARCHITECTURE, AutofocusClassifier.classifySync("ARCHITECTURE REVIEW"))
        assertEquals(AutofocusLevel.MODULE, AutofocusClassifier.classifySync("REFACTOR MODULE"))
        assertEquals(AutofocusLevel.IMPLEMENTATION, AutofocusClassifier.classifySync("COMPILATION ERROR"))
    }

    @Test
    fun `classifySync detects générer formation as BIG_PICTURE`() {
        val result = AutofocusClassifier.classifySync("générer une formation FPA complète")
        assertEquals(AutofocusLevel.BIG_PICTURE, result)
    }

    @Test
    fun `classifySync detects migrate groupId as ARCHITECTURE`() {
        val result = AutofocusClassifier.classifySync("migrate groupId from com.cheroliv to education.cccp")
        assertEquals(AutofocusLevel.ARCHITECTURE, result)
    }
}
