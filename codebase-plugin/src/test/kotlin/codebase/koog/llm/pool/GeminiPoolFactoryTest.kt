package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeminiPoolFactoryTest {

    @Test
    fun `fromEnvVarMapping should build pool from explicit key list`() {
        val keys = listOf("key-AAA", "key-BBB", "key-CCC")
        val pool = GeminiPoolFactory.fromKeys(keys)
        assertEquals(3, pool.size())
        assertEquals("gemini-key-0", pool.instances()[0].id)
        assertEquals("gemini-key-1", pool.instances()[1].id)
        assertEquals("gemini-key-2", pool.instances()[2].id)
    }

    @Test
    fun `fromKeys should embed key in baseUrl query param`() {
        val pool = GeminiPoolFactory.fromKeys(listOf("secret-1"))
        val inst = pool.instances()[0]
        assertTrue(inst.baseUrl.contains("key=secret-1"), "baseUrl should contain the key. Got: ${inst.baseUrl}")
    }

    @Test
    fun `fromKeys should assign default model gemini-2_5-flash`() {
        val pool = GeminiPoolFactory.fromKeys(listOf("k1"))
        assertEquals("gemini-2.5-flash", pool.instances()[0].model)
    }

    @Test
    fun `fromKeys should accept custom model`() {
        val pool = GeminiPoolFactory.fromKeys(listOf("k1"), model = "gemini-2.5-pro")
        assertEquals("gemini-2.5-pro", pool.instances()[0].model)
    }

    @Test
    fun `fromKeys should use ROUND_ROBIN by default`() {
        val pool = GeminiPoolFactory.fromKeys(listOf("k1", "k2"))
        assertEquals("gemini-key-0", pool.nextInstance().id)
        assertEquals("gemini-key-1", pool.nextInstance().id)
    }

    @Test
    fun `fromEnvVars should read GEMINI_API_KEY_N from provided env map`() {
        val env = mapOf(
            "GEMINI_API_KEY_1" to "env-key-1",
            "GEMINI_API_KEY_2" to "env-key-2",
            "GEMINI_API_KEY_3" to "env-key-3"
        )
        val pool = GeminiPoolFactory.fromEnvVars(env)
        assertEquals(3, pool.size())
        val ids = pool.instances().map { it.id }.toSet()
        assertTrue("gemini-key-0" in ids)
        assertTrue("gemini-key-1" in ids)
        assertTrue("gemini-key-2" in ids)
    }

    @Test
    fun `fromEnvVars should skip empty or missing keys`() {
        val env = mapOf(
            "GEMINI_API_KEY_1" to "env-key-1",
            "GEMINI_API_KEY_2" to "",
            "GEMINI_API_KEY_3" to "env-key-3"
        )
        val pool = GeminiPoolFactory.fromEnvVars(env)
        assertEquals(2, pool.size())
    }

    @Test
    fun `fromEnvVars should return empty pool when no keys present`() {
        val pool = GeminiPoolFactory.fromEnvVars(emptyMap())
        assertEquals(0, pool.size())
    }

    @Test
    fun `fromEnvVars should detect keys in order 1 to N`() {
        val env = mapOf(
            "GEMINI_API_KEY_3" to "third",
            "GEMINI_API_KEY_1" to "first",
            "GEMINI_API_KEY_2" to "second"
        )
        val pool = GeminiPoolFactory.fromEnvVars(env)
        val keys = pool.instances().map { it.baseUrl.substringAfter("key=") }
        assertEquals(listOf("first", "second", "third"), keys)
    }

    @Test
    fun `fromEnvVars should accept custom model`() {
        val env = mapOf("GEMINI_API_KEY_1" to "k1")
        val pool = GeminiPoolFactory.fromEnvVars(env, model = "gemini-2.5-pro")
        assertEquals("gemini-2.5-pro", pool.instances()[0].model)
    }

    @Test
    fun `fromKeys should configure default quota`() {
        val pool = GeminiPoolFactory.fromKeys(listOf("k1"))
        val inst = pool.instances()[0]
        assertEquals(QuotaConfig().limitValue, inst.quota.limitValue)
        assertEquals(ResetPolicy.NEVER, inst.quota.resetPolicy)
    }
}