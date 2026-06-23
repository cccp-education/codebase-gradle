package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance
import contracts.llmpool.RotationStrategy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeminiMultiAccountPoolTest {

    private fun pool(vararg keys: String): GeminiKeyPool =
        GeminiPoolFactory.fromKeys(keys.toList())

    @Test
    fun `multi-account pool with 2 accounts should have 2 sub-pools`() {
        val multi = GeminiMultiAccountPool(mapOf(
            "account-A" to pool("key-A1", "key-A2"),
            "account-B" to pool("key-B1", "key-B2")
        ))
        assertEquals(2, multi.accountCount())
        assertEquals(4, multi.totalSize())
    }

    @Test
    fun `nextInstance should rotate across accounts`() {
        val multi = GeminiMultiAccountPool(mapOf(
            "account-A" to pool("key-A1", "key-A2"),
            "account-B" to pool("key-B1", "key-B2")
        ))
        val first = multi.nextInstance()
        val second = multi.nextInstance()
        val third = multi.nextInstance()
        val fourth = multi.nextInstance()
        assertEquals("gemini-key-0", first.id)
        assertEquals("gemini-key-0", second.id)
        assertEquals("gemini-key-1", third.id)
        assertEquals("gemini-key-1", fourth.id)
    }

    @Test
    fun `markRateLimited should skip key in the right account`() {
        val accountA = pool("key-A1", "key-A2")
        val multi = GeminiMultiAccountPool(mapOf(
            "account-A" to accountA,
            "account-B" to pool("key-B1")
        ))
        val inst = accountA.instances()[0]
        multi.markRateLimited(inst)
        assertTrue(multi.isRateLimited(inst))
        val next = multi.nextInstance()
        assertTrue(next.id != inst.id, "Rate-limited instance should be skipped")
    }

    @Test
    fun `resetUsage should clear all accounts`() {
        val accountA = pool("key-A1")
        val multi = GeminiMultiAccountPool(mapOf("account-A" to accountA))
        val inst = accountA.instances()[0]
        multi.markRateLimited(inst)
        assertTrue(multi.isRateLimited(inst))
        multi.resetUsage()
        assertFalse(multi.isRateLimited(inst))
    }

    @Test
    fun `empty multi-account pool should throw on nextInstance`() {
        val multi = GeminiMultiAccountPool(emptyMap())
        assertFailsWith<IllegalStateException> {
            multi.nextInstance()
        }
    }

    @Test
    fun `fromEnvVarsMultiAccount should map GEMINI_ACCOUNT_N_API_KEY_M pattern`() {
        val env = mapOf(
            "GEMINI_ACCOUNT_1_API_KEY_1" to "a1k1",
            "GEMINI_ACCOUNT_1_API_KEY_2" to "a1k2",
            "GEMINI_ACCOUNT_2_API_KEY_1" to "a2k1"
        )
        val multi = GeminiMultiAccountPool.fromEnvVars(env)
        assertEquals(2, multi.accountCount())
        assertEquals(3, multi.totalSize())
    }

    @Test
    fun `fromEnvVarsMultiAccount should skip empty keys`() {
        val env = mapOf(
            "GEMINI_ACCOUNT_1_API_KEY_1" to "a1k1",
            "GEMINI_ACCOUNT_1_API_KEY_2" to "",
            "GEMINI_ACCOUNT_2_API_KEY_1" to "a2k1"
        )
        val multi = GeminiMultiAccountPool.fromEnvVars(env)
        assertEquals(2, multi.accountCount())
        assertEquals(2, multi.totalSize())
    }

    @Test
    fun `fromEnvVarsMultiAccount should return empty pool when no env vars`() {
        val multi = GeminiMultiAccountPool.fromEnvVars(emptyMap())
        assertEquals(0, multi.accountCount())
    }
}