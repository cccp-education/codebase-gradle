package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OllamaInstanceFactoryTest {

    @Test
    fun `create should build single instance`() {
        val instances = OllamaInstanceFactory.create(11437..11437)

        assertEquals(1, instances.size)
        assertEquals(expectedInstance(11437, "ollama-11437", "gpt-oss:120b-cloud"), instances[0])
    }

    @Test
    fun `create should assign distinct ports to distinct instances`() {
        val instances = OllamaInstanceFactory.create(11437..11438)

        assertEquals(2, instances.size)
        assertEquals(11437, extractPort(instances[0]))
        assertEquals(11438, extractPort(instances[1]))
        assertEquals("http://localhost:11437", instances[0].baseUrl)
        assertEquals("http://localhost:11438", instances[1].baseUrl)
    }

    @Test
    fun `create should tag each instance with a volume tag matching its id`() {
        val instances = OllamaInstanceFactory.create(11437..11439)

        for (instance in instances) {
            assertEquals(instance.id, instance.volumeTag,
                "volumeTag must match id for SSH volume identity")
        }
    }

    @Test
    fun `create should cycle through all 2 authorized models`() {
        val instances = OllamaInstanceFactory.create(11437..11438)

        val expectedModels = listOf(
            "gpt-oss:120b-cloud",
            "gemma4:31b-cloud"
        )
        assertEquals(expectedModels, instances.map { it.model })
    }

    @Test
    fun `create should cycle models after the 2nd instance`() {
        val instances = OllamaInstanceFactory.create(11437..11439)

        assertEquals(3, instances.size)
        assertEquals("gpt-oss:120b-cloud", instances[0].model)
        assertEquals("gpt-oss:120b-cloud", instances[2].model,
            "3rd instance should wrap back to the first authorized model")
    }

    @Test
    fun `create default should cover all 29 authorized ports`() {
        val instances = OllamaInstanceFactory.create()

        assertEquals(29, instances.size)
        val ports = instances.map { extractPort(it) }
        assertEquals((11437..11465).toList(), ports)
    }

    @Test
    fun `create default should use only authorized models`() {
        val instances = OllamaInstanceFactory.create()

        val models = instances.map { it.model }.toSet()
        assertEquals(2, models.size)
        for (authorized in OllamaInstanceFactory.AUTHORIZED_MODELS) {
            assertTrue(authorized in models, "Authorized model '$authorized' missing")
        }
    }

    @Test
    fun `create default should include gemma4 31b cloud`() {
        val instances = OllamaInstanceFactory.create()

        val models = instances.map { it.model }
        assertTrue("gemma4:31b-cloud" in models, "gemma4:31b-cloud must be present in the pool")
    }

    private fun expectedInstance(port: Int, id: String, model: String): LlmInstance =
        LlmInstance(
            id = id,
            baseUrl = "http://localhost:$port",
            model = model,
            volumeTag = id
        )

    private fun extractPort(instance: LlmInstance): Int =
        instance.baseUrl.removePrefix("http://localhost:").toInt()
}
