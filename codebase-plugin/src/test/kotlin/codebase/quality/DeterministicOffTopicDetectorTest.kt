package codebase.quality

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterministicOffTopicDetectorTest {

    private val detector = DeterministicOffTopicDetector()

    @Test
    fun `CODING output with Kotlin keywords is on topic`() {
        val result = detector.check(
            "class MainActivity : AppCompatActivity() { val list = mutableListOf<String>() }",
            Domain.CODING,
            QualityGateConfig()
        )
        assertEquals(QualityVerdict.PASS, result.verdict)
        assertTrue(result.score >= 0.60)
    }

    @Test
    fun `CODING output with Gradle keywords is on topic`() {
        val result = detector.check(
            "plugins { kotlin(\"jvm\") } dependencies { implementation(\"lib\") }",
            Domain.CODING,
            QualityGateConfig()
        )
        assertEquals(QualityVerdict.PASS, result.verdict)
        assertTrue(result.score >= 0.50)
    }

    @Test
    fun `CONTENT output with editorial keywords is on topic`() {
        val result = detector.check(
            "Objectif : structurer le guide de publication selon le plan éditorial.",
            Domain.CONTENT,
            QualityGateConfig()
        )
        assertEquals(QualityVerdict.PASS, result.verdict)
        assertTrue(result.score >= 0.50)
    }

    @Test
    fun `CODING output about cuisine is off topic`() {
        val result = detector.check(
            "La recette du gâteau au chocolat nécessite 200g de farine.",
            Domain.CODING,
            QualityGateConfig()
        )
        assertTrue(result.verdict.severity >= QualityVerdict.NEEDS_FIX.severity)
        assertTrue(result.score < 0.30)
    }

    @Test
    fun `CONTENT output about sports is off topic`() {
        val result = detector.check(
            "Le match de football a été gagné par l'équipe locale.",
            Domain.CONTENT,
            QualityGateConfig()
        )
        assertTrue(result.verdict.severity >= QualityVerdict.NEEDS_FIX.severity)
        assertTrue(result.score < 0.30)
    }

    @Test
    fun `CODING output about content production is off topic for CODING domain`() {
        val result = detector.check(
            "La rédaction professionnelle nécessite une relecture éditoriale continue.",
            Domain.CODING,
            QualityGateConfig()
        )
        assertTrue(result.verdict.severity >= QualityVerdict.NEEDS_FIX.severity)
    }

    @Test
    fun `CONTENT output about Spring Boot is off topic for CONTENT domain`() {
        val result = detector.check(
            "@SpringBootApplication class App { fun main() = runApplication<App>(*args) }",
            Domain.CONTENT,
            QualityGateConfig()
        )
        assertTrue(result.verdict.severity >= QualityVerdict.NEEDS_FIX.severity)
    }

    @Test
    fun `empty text passes for any domain`() {
        val result = detector.check("", Domain.CODING, QualityGateConfig())
        assertEquals(QualityVerdict.PASS, result.verdict)
        assertEquals(1.0, result.score)
    }

    @Test
    fun `generic programming text is on topic for CODING`() {
        val result = detector.check(
            "fun calculateSum(a: Int, b: Int): Int = a + b",
            Domain.CODING,
            QualityGateConfig()
        )
        assertEquals(QualityVerdict.PASS, result.verdict)
    }

    @Test
    fun `generic content text is on topic for CONTENT`() {
        val result = detector.check(
            "Le lecteur doit suivre la séquence du module de documentation.",
            Domain.CONTENT,
            QualityGateConfig()
        )
        assertEquals(QualityVerdict.PASS, result.verdict)
    }

    @Test
    fun `CONTENT text with publishing reference is strongly on topic`() {
        val result = detector.check(
            "Conforme au plan éditorial, publication et diffusion de l'article.",
            Domain.CONTENT,
            QualityGateConfig()
        )
        assertEquals(QualityVerdict.PASS, result.verdict)
        assertTrue(result.score >= 0.70)
    }

    @Test
    fun `details mention expected domain`() {
        val result = detector.check("Kotlin is great", Domain.CODING, QualityGateConfig())
        assertTrue(result.details.lowercase().contains("coding"))
    }

    @Test
    fun `score is always between 0 and 1`() {
        val texts = listOf(
            "Spring Boot Gradle Kotlin PostgreSQL Docker" to Domain.CODING,
            "editorial outline publication article guide" to Domain.CONTENT,
            "recette cuisine jardinage météo" to Domain.CODING,
            "football tennis natation" to Domain.CONTENT
        )
        for ((text, domain) in texts) {
            val result = detector.check(text, domain, QualityGateConfig())
            assertTrue(result.score in 0.0..1.0, "score ${result.score} out of range for $domain: $text")
        }
    }
}
