package codebase.koog.expert

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ExpertExposureContractsTest {

    @Test
    fun `ExpertExposureManifest holds domain registrations with Ollama endpoints`() {
        val entries = listOf(
            ExpertExposureEntry(
                domain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem"),
                modelName = "gpt-oss:120b-cloud",
                baseUrl = "http://localhost:11437",
                timeoutSeconds = 120
            ),
            ExpertExposureEntry(
                domain = ExpertDomain("docs", "Documentation, AsciiDoc, technical writing"),
                modelName = "gpt-oss:20b-cloud",
                baseUrl = "http://localhost:11438",
                timeoutSeconds = 90
            )
        )
        val manifest = ExpertExposureManifest(
            version = "1.0",
            generatedAt = "2026-06-16T10:00:00Z",
            experts = entries
        )

        assertEquals("1.0", manifest.version)
        assertEquals(2, manifest.experts.size)
        assertEquals("kotlin", manifest.experts[0].domain.name)
        assertEquals("gpt-oss:120b-cloud", manifest.experts[0].modelName)
        assertEquals("http://localhost:11437", manifest.experts[0].baseUrl)
        assertEquals(120, manifest.experts[0].timeoutSeconds)
    }

    @Test
    fun `ExpertExposureManifest empty experts list`() {
        val manifest = ExpertExposureManifest(
            version = "1.0",
            generatedAt = "2026-06-16T10:00:00Z",
            experts = emptyList()
        )

        assertTrue(manifest.experts.isEmpty())
        assertEquals("1.0", manifest.version)
    }

    @Test
    fun `ExpertExposureEntry equality`() {
        val a = ExpertExposureEntry(
            domain = ExpertDomain("kotlin", "Kotlin"),
            modelName = "gpt-oss:120b-cloud",
            baseUrl = "http://localhost:11437",
            timeoutSeconds = 120
        )
        val b = ExpertExposureEntry(
            domain = ExpertDomain("kotlin", "Kotlin"),
            modelName = "gpt-oss:120b-cloud",
            baseUrl = "http://localhost:11437",
            timeoutSeconds = 120
        )
        val c = ExpertExposureEntry(
            domain = ExpertDomain("docs", "Docs"),
            modelName = "gpt-oss:20b-cloud",
            baseUrl = "http://localhost:11438",
            timeoutSeconds = 90
        )

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }

    @Test
    fun `ExpertExposureConfig holds exposure settings`() {
        val config = ExpertExposureConfig(
            domains = listOf("kotlin", "docs"),
            outputFormat = "json",
            anonymizeEndpoints = true,
            outputFile = "build/experts/exposure-manifest.json"
        )

        assertEquals(listOf("kotlin", "docs"), config.domains)
        assertEquals("json", config.outputFormat)
        assertTrue(config.anonymizeEndpoints)
        assertEquals("build/experts/exposure-manifest.json", config.outputFile)
    }

    @Test
    fun `ExpertExposureConfig defaults`() {
        val config = ExpertExposureConfig()

        assertTrue(config.domains.isEmpty())
        assertEquals("json", config.outputFormat)
        assertTrue(config.anonymizeEndpoints)
        assertEquals("build/experts/exposure-manifest.json", config.outputFile)
    }

    @Test
    fun `ExpertExposureConfig with all domains exposes everything`() {
        val config = ExpertExposureConfig(
            domains = emptyList(),
            outputFormat = "yaml",
            anonymizeEndpoints = false,
            outputFile = "build/experts/manifest.yaml"
        )

        assertTrue(config.domains.isEmpty())
        assertEquals("yaml", config.outputFormat)
        assertFalse(config.anonymizeEndpoints)
    }

    @Test
    fun `ExpertExposureEntry from ExpertRegistration mapping`() {
        val reg = ExpertRegistration(
            domain = ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem"),
            modelName = "gpt-oss:120b-cloud",
            baseUrl = "http://localhost:11437",
            timeoutSeconds = 120
        )

        val entry = ExpertExposureEntry.from(reg)

        assertEquals(reg.domain, entry.domain)
        assertEquals(reg.modelName, entry.modelName)
        assertEquals(reg.baseUrl, entry.baseUrl)
        assertEquals(reg.timeoutSeconds, entry.timeoutSeconds)
    }

    @Test
    fun `ExpertExposureEntry anonymized hides baseUrl`() {
        val entry = ExpertExposureEntry(
            domain = ExpertDomain("kotlin", "Kotlin"),
            modelName = "gpt-oss:120b-cloud",
            baseUrl = "http://localhost:11437",
            timeoutSeconds = 120
        )

        val anonymized = entry.anonymize()

        assertEquals(entry.domain, anonymized.domain)
        assertEquals(entry.modelName, anonymized.modelName)
        assertEquals("***anonymized***", anonymized.baseUrl)
        assertEquals(entry.timeoutSeconds, anonymized.timeoutSeconds)
    }

    @Test
    fun `ExpertExposureManifest anonymized hides all endpoints`() {
        val entries = listOf(
            ExpertExposureEntry(
                domain = ExpertDomain("kotlin", "Kotlin"),
                modelName = "gpt-oss:120b-cloud",
                baseUrl = "http://localhost:11437",
                timeoutSeconds = 120
            ),
            ExpertExposureEntry(
                domain = ExpertDomain("docs", "Docs"),
                modelName = "gpt-oss:20b-cloud",
                baseUrl = "http://192.168.1.100:11438",
                timeoutSeconds = 90
            )
        )
        val manifest = ExpertExposureManifest(
            version = "1.0",
            generatedAt = "2026-06-16T10:00:00Z",
            experts = entries
        )

        val anonymized = manifest.anonymize()

        assertEquals("1.0", anonymized.version)
        assertEquals(2, anonymized.experts.size)
        anonymized.experts.forEach { entry ->
            assertEquals("***anonymized***", entry.baseUrl)
        }
    }
}
