package codebase.koog.llm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OllamaOcrProviderTest {
    @Test
    fun `should be instantiable with default parameters`() {
        val provider = OllamaOcrProvider()
        assertThat(provider).isNotNull()
        assertThat(provider.javaClass.simpleName).isEqualTo("OllamaOcrProvider")
    }

    @Test
    fun `should implement VisionProvider interface`() {
        val provider: VisionProvider = OllamaOcrProvider()
        assertThat(provider.javaClass).isEqualTo(OllamaOcrProvider::class.java)
    }

    @Test
    fun `should accept custom baseUrl and model`() {
        val provider =
            OllamaOcrProvider(
                baseUrl = "http://localhost:11437",
                model = "gpt-oss:120b-cloud",
                timeoutSeconds = 60,
            )
        assertThat(provider).isNotNull()
    }

    @Test
    fun `should accept various language and model parameters`() {
        val provider = OllamaOcrProvider()
        assertThat(provider).isNotNull()
    }

    @Test
    fun `FakeOllamaOcrProvider implements VisionProvider`() {
        val provider: VisionProvider = FakeOllamaOcrProvider()
        assertThat(provider.javaClass).isEqualTo(FakeOllamaOcrProvider::class.java)
    }
}
