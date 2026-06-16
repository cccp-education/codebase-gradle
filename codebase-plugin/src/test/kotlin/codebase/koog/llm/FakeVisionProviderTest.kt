package codebase.koog.llm

import codebase.koog.llm.pool.GeminiKeyPool
import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class FakeVisionProviderTest {

    @Test
    fun `processImage returns structured AsciiDoc without network call`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(100, 50)
        val result = provider.processImage(fakePng, "image/png", "fr")

        assertTrue(result.contains("= Titre Principal"), "Should have FR title, got: $result")
        assertTrue(result.contains("FakeVisionProvider"))
        assertTrue(result.contains("Confiance: haute"))
        assertTrue(result.contains("Section 1"))
        assertTrue(result.contains("Section 2"))
        assertTrue(result.contains("| Colonne A"))
        assertTrue(result.contains("| Valeur 1"))
        assertTrue(result.contains("Premier élément de liste"))
        assertTrue(result.contains("caractères gras"))
    }

    @Test
    fun `processImage adapts title to language`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(50, 50)

        val fr = provider.processImage(fakePng, "image/png", "fr")
        assertTrue(fr.contains("= Titre Principal"))

        val en = provider.processImage(fakePng, "image/png", "en")
        assertTrue(en.contains("= Main Title"))

        val de = provider.processImage(fakePng, "image/png", "de")
        assertTrue(de.contains("= Haupttitel"))
    }

    @Test
    fun `processImage reports image size in output`() = runBlocking {
        val provider = FakeVisionProvider()
        val bytes = ByteArray(42) { 'x'.code.toByte() }
        val result = provider.processImage(bytes, "image/jpeg", "fr")
        assertTrue(result.contains("42 bytes"))
    }

    @Test
    fun `processImage reports model name in comment`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(32, 32)
        val result = provider.processImage(fakePng, "image/png", "en", model = "gemini-2.5-pro")
        assertTrue(result.contains("gemini-2.5-pro"))
    }

    @Test
    fun `processImage with default language is french`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(16, 16)
        val result = provider.processImage(fakePng, "image/gif")
        assertTrue(result.contains("Titre Principal"))
        assertTrue(result.contains("Langue: fr"))
    }

    @Test
    fun `processImage reports Gemini key id in output`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(64, 64)
        val result = provider.processImage(fakePng, "image/png", "fr")
        assertTrue(result.contains("Gemini key: test-gemini-key-1"))
        assertTrue(result.contains("Pool size: 2 keys"))
    }

    @Test
    fun `processImage rotates keys ROUND_ROBIN`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(32, 32)

        val r1 = provider.processImage(fakePng, "image/png", "fr")
        assertTrue(r1.contains("Gemini key: test-gemini-key-1"))
        assertEquals("test-gemini-key-1", provider.lastUsedKeyId)

        val r2 = provider.processImage(fakePng, "image/png", "fr")
        assertTrue(r2.contains("Gemini key: test-gemini-key-2"))
        assertEquals("test-gemini-key-2", provider.lastUsedKeyId)

        val r3 = provider.processImage(fakePng, "image/png", "fr")
        assertTrue(r3.contains("Gemini key: test-gemini-key-1"))
        assertEquals("test-gemini-key-1", provider.lastUsedKeyId)
    }

    @Test
    fun `keyPool tracks usage and detects quota exceeded`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(16, 16)

        repeat(9) { provider.processImage(fakePng, "image/png", "fr") }

        val key1 = provider.keyPool.instances().first { it.id == "test-gemini-key-1" }
        assertTrue(provider.keyPool.isQuotaExceeded(key1), "Key 1 should be quota exceeded after 5 uses (threshold 50% of 10 = 5)")
    }

    @Test
    fun `keyPool skips quota-exceeded key and uses next available`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(16, 16)

        repeat(9) { provider.processImage(fakePng, "image/png", "fr") }

        val key1 = provider.keyPool.instances().first { it.id == "test-gemini-key-1" }
        val key2 = provider.keyPool.instances().first { it.id == "test-gemini-key-2" }
        assertTrue(provider.keyPool.isQuotaExceeded(key1))
        assertFalse(provider.keyPool.isQuotaExceeded(key2))

        val result = provider.processImage(fakePng, "image/png", "fr")
        assertTrue(result.contains("Gemini key: test-gemini-key-2"), "Should skip key-1 and use key-2")
    }

    @Test
    fun `keyPool resetUsage clears all counters`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(16, 16)

        repeat(9) { provider.processImage(fakePng, "image/png", "fr") }
        val key1 = provider.keyPool.instances().first { it.id == "test-gemini-key-1" }
        assertTrue(provider.keyPool.isQuotaExceeded(key1))

        provider.keyPool.resetUsage()
        assertFalse(provider.keyPool.isQuotaExceeded(key1))
    }

    @Test
    fun `custom keyPool can be injected with test secrets`() = runBlocking {
        val customPool = GeminiKeyPool(
            listOf(
                LlmInstance(
                    id = "secret-key-alpha",
                    baseUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-pro:generateContent?key=alpha-secret",
                    model = "gemini-2.5-pro",
                    quota = QuotaConfig(limitValue = 3, thresholdPercent = 50, resetPolicy = ResetPolicy.NEVER)
                ),
                LlmInstance(
                    id = "secret-key-beta",
                    baseUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-pro:generateContent?key=beta-secret",
                    model = "gemini-2.5-pro",
                    quota = QuotaConfig(limitValue = 3, thresholdPercent = 50, resetPolicy = ResetPolicy.NEVER)
                )
            ),
            rotationStrategy = RotationStrategy.LEAST_USED
        )
        val provider = FakeVisionProvider(keyPool = customPool)
        val fakePng = createFakeImage(32, 32)

        val r1 = provider.processImage(fakePng, "image/png", "fr")
        assertTrue(r1.contains("Gemini key: secret-key-alpha"))
        assertTrue(r1.contains("Pool size: 2 keys"))

        val r2 = provider.processImage(fakePng, "image/png", "fr")
        assertTrue(r2.contains("Gemini key: secret-key-beta"))
    }

    @Test
    fun `lastUsedKeyId is null before first call`() {
        val provider = FakeVisionProvider()
        assertEquals(null, provider.lastUsedKeyId)
    }

    @Test
    fun `lastUsedKeyId is set after processImage`() = runBlocking {
        val provider = FakeVisionProvider()
        val fakePng = createFakeImage(16, 16)
        provider.processImage(fakePng, "image/png", "fr")
        assertNotNull(provider.lastUsedKeyId)
    }

    private fun createFakeImage(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val tempFile = File.createTempFile("fake-ocr", ".png")
        ImageIO.write(image, "png", tempFile)
        val bytes = tempFile.readBytes()
        tempFile.delete()
        return bytes
    }
}
