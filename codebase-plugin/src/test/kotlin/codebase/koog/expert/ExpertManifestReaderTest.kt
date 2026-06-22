package codebase.koog.expert

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ExpertManifestReaderTest {

    private fun writeManifest(content: String): File {
        val tmp = File.createTempFile("manifest-reader", ".json")
        tmp.writeText(content)
        return tmp
    }

    @Test
    fun `read returns ExpertExposureManifest parsed from JSON`() {
        val json = """
            {
              "version": "1.0",
              "generatedAt": "2026-06-22T20:00:00Z",
              "experts": [
                {"domain": "kotlin", "label": "kotlin domain", "modelName": "gpt-oss:120b-cloud", "baseUrl": "http://localhost:11437", "timeoutSeconds": 120},
                {"domain": "docs", "label": "docs domain", "modelName": "qwen3-vl:235b-cloud", "baseUrl": "***anonymized***", "timeoutSeconds": 90}
              ]
            }
        """.trimIndent()
        val file = writeManifest(json)

        val manifest = ExpertManifestReader.read(file)

        assertEquals("1.0", manifest.version)
        assertEquals("2026-06-22T20:00:00Z", manifest.generatedAt)
        assertEquals(2, manifest.experts.size)
        val first = manifest.experts.first()
        assertEquals(ExpertDomain("kotlin", "kotlin domain"), first.domain)
        assertEquals("gpt-oss:120b-cloud", first.modelName)
        assertEquals("http://localhost:11437", first.baseUrl)
        assertEquals(120L, first.timeoutSeconds)
    }

    @Test
    fun `read accepts empty experts list`() {
        val json = """{"version":"1.0","generatedAt":"t","experts":[]}"""
        val file = writeManifest(json)

        val manifest = ExpertManifestReader.read(file)

        assertTrue(manifest.experts.isEmpty())
    }

    @Test
    fun `read preserves anonymized baseUrl`() {
        val json = """
            {"version":"1.0","generatedAt":"t","experts":[
              {"domain":"docs","label":"docs domain","modelName":"m","baseUrl":"***anonymized***","timeoutSeconds":30}
            ]}
        """.trimIndent()
        val file = writeManifest(json)

        val manifest = ExpertManifestReader.read(file)

        assertEquals("***anonymized***", manifest.experts.single().baseUrl)
    }

    @Test
    fun `findByDomain returns matching entry`() {
        val json = """
            {"version":"1.0","generatedAt":"t","experts":[
              {"domain":"kotlin","label":"kotlin domain","modelName":"m1","baseUrl":"u1","timeoutSeconds":10},
              {"domain":"docs","label":"docs domain","modelName":"m2","baseUrl":"u2","timeoutSeconds":20}
            ]}
        """.trimIndent()
        val file = writeManifest(json)

        val entry = ExpertManifestReader.read(file).findByDomain("docs")

        assertNotNull(entry)
        assertEquals("m2", entry.modelName)
    }

    @Test
    fun `findByDomain returns null when missing`() {
        val json = """{"version":"1.0","generatedAt":"t","experts":[]}"""
        val file = writeManifest(json)

        val entry = ExpertManifestReader.read(file).findByDomain("missing")

        assertNull(entry)
    }

    @Test
    fun `read throws on malformed JSON`() {
        val file = writeManifest("{not json")

        assertFailsWith<ExpertManifestException> { ExpertManifestReader.read(file) }
    }

    @Test
    fun `read throws on missing file`() {
        val missing = File.createTempFile("missing", ".json")
        missing.delete()

        assertFailsWith<ExpertManifestException> { ExpertManifestReader.read(missing) }
    }

    @Test
    fun `readOrEmpty returns empty manifest when file missing`() {
        val missing = File.createTempFile("missing", ".json")
        missing.delete()

        val manifest = ExpertManifestReader.readOrEmpty(missing)

        assertTrue(manifest.experts.isEmpty())
    }
}