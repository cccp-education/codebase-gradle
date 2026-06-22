package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy
import kotlin.test.*

class OllamaPoolKeyAdapterTest {

    private fun instance(id: String, port: Int, limit: Long = 10, threshold: Int = 50) =
        LlmInstance(
            id = id,
            baseUrl = "http://localhost:$port",
            model = "gpt-oss:120b-cloud",
            quota = QuotaConfig(limitValue = limit, thresholdPercent = threshold, resetPolicy = ResetPolicy.NEVER)
        )

    @Test
    fun `nextInstance should skip inactive instance`() {
        val pool = OllamaPool(
            listOf(instance("a", 11437), instance("b", 11438)),
            rotationStrategy = RotationStrategy.ROUND_ROBIN
        )
        val adapter = OllamaPoolKeyAdapter(pool)

        adapter.markInactiveForTest("a")

        assertEquals("b", adapter.nextInstance().id)
        assertEquals("b", adapter.nextInstance().id)
    }

    @Test
    fun `nextInstance should skip quota exceeded instance`() {
        val pool = OllamaPool(
            listOf(instance("a", 11437, limit = 2, threshold = 50), instance("b", 11438)),
            rotationStrategy = RotationStrategy.ROUND_ROBIN
        )
        val adapter = OllamaPoolKeyAdapter(pool)

        repeat(2) { adapter.nextInstance() } // a puis b ; a atteint son seuil

        assertEquals("b", adapter.nextInstance().id)
    }

    @Test
    fun `nextInstance should throw when all instances unavailable`() {
        val pool = OllamaPool(listOf(instance("a", 11437)), RotationStrategy.ROUND_ROBIN)
        val adapter = OllamaPoolKeyAdapter(pool)
        adapter.markInactiveForTest("a")

        assertFailsWith<IllegalStateException> {
            adapter.nextInstance()
        }
    }

    @Test
    fun `callWithRotation should rotate on quota exceeded`() {
        val pool = OllamaPool(
            listOf(instance("a", 11437), instance("b", 11438)),
            rotationStrategy = RotationStrategy.ROUND_ROBIN
        )
        val adapter = OllamaPoolKeyAdapter(pool)
        var calls = 0

        val result = adapter.callWithRotation { instance ->
            calls++
            if (instance.id == "a") {
                throw RuntimeException("quota exceeded for ${instance.id}")
            }
            "ok-${instance.id}"
        }

        assertEquals("ok-b", result)
        assertEquals(2, calls)
    }

    @Test
    fun `callWithRotation should mark connection refused as inactive and rotate`() {
        val pool = OllamaPool(
            listOf(instance("a", 11437), instance("b", 11438)),
            rotationStrategy = RotationStrategy.ROUND_ROBIN
        )
        val adapter = OllamaPoolKeyAdapter(pool)
        var calls = 0

        val result = adapter.callWithRotation { instance ->
            calls++
            if (instance.id == "a") {
                throw RuntimeException("Connection refused")
            }
            "ok-${instance.id}"
        }

        assertEquals("ok-b", result)
        assertTrue(adapter.isInactive("a"))
        assertEquals(2, calls)
    }

    @Test
    fun `callWithRotation should throw IllegalStateException when pool exhausted`() {
        val pool = OllamaPool(listOf(instance("a", 11437), instance("b", 11438)), RotationStrategy.ROUND_ROBIN)
        val adapter = OllamaPoolKeyAdapter(pool)
        var calls = 0

        assertFailsWith<IllegalStateException> {
            adapter.callWithRotation { instance ->
                calls++
                throw RuntimeException("quota exceeded for ${instance.id}")
            }
        }

        assertEquals(2, calls, "Should attempt both instances")
    }

    @Test
    fun `callWithRotation should propagate unrelated exceptions immediately`() {
        val pool = OllamaPool(listOf(instance("a", 11437)), RotationStrategy.ROUND_ROBIN)
        val adapter = OllamaPoolKeyAdapter(pool)

        assertFailsWith<IllegalArgumentException> {
            adapter.callWithRotation { _ ->
                throw IllegalArgumentException("unexpected")
            }
        }
    }

    @Test
    fun `resetInactive should reactivate all instances`() {
        val pool = OllamaPool(listOf(instance("a", 11437), instance("b", 11438)), RotationStrategy.ROUND_ROBIN)
        val adapter = OllamaPoolKeyAdapter(pool)
        adapter.markInactiveForTest("a")

        adapter.resetInactive()

        assertFalse(adapter.isInactive("a"))
    }

    @Test
    fun `callWithRotation should succeed on first attempt when instance is healthy`() {
        val pool = OllamaPool(listOf(instance("a", 11437)), RotationStrategy.ROUND_ROBIN)
        val adapter = OllamaPoolKeyAdapter(pool)

        val result = adapter.callWithRotation { "success" }

        assertEquals("success", result)
    }

    @Test
    fun `nextInstance should delegate size and instances to wrapped pool`() {
        val pool = OllamaPool(listOf(instance("a", 11437), instance("b", 11438)), RotationStrategy.ROUND_ROBIN)
        val adapter = OllamaPoolKeyAdapter(pool)

        assertEquals(2, adapter.size())
        assertEquals(2, adapter.instances().size)
    }

    private fun OllamaPoolKeyAdapter.markInactiveForTest(id: String) {
        OllamaPoolKeyAdapter::class.java.getDeclaredField("inactive").apply {
            isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (get(this@markInactiveForTest) as MutableSet<String>).add(id)
        }
    }
}
