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
    fun `create should cycle through all 5 authorized models`() {
        val instances = OllamaInstanceFactory.create(11437..11441)

        val expectedModels = listOf(
            "gpt-oss:120b-cloud",
            "gpt-oss:20b-cloud",
            "qwen3-coder-next:cloud",
            "qwen3-next:80b-cloud",
            "qwen3-coder:480b-cloud"
        )
        assertEquals(expectedModels, instances.map { it.model })
    }

    @Test
    fun `create should cycle models after the 5th instance`() {
        val instances = OllamaInstanceFactory.create(11437..11442)

        assertEquals(6, instances.size)
        assertEquals("gpt-oss:120b-cloud", instances[0].model)
        assertEquals("gpt-oss:120b-cloud", instances[5].model,
            "6th instance should wrap back to the first authorized model")
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
        assertEquals(5, models.size)
        for (authorized in OllamaInstanceFactory.AUTHORIZED_MODELS) {
            assertTrue(authorized in models, "Authorized model '$authorized' missing")
        }
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
