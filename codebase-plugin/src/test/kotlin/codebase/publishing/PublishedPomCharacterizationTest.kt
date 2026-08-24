package codebase.publishing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import java.io.File

class PublishedPomCharacterizationTest {

    companion object {
        private const val GROUP_PATH = "education/cccp"
        private const val ARTIFACT = "codebase-plugin"
        private const val VERSION = "0.0.9"
    }

    private val pomFile: File by lazy {
        val m2 = File(System.getProperty("user.home"), ".m2/repository")
        File(m2, "$GROUP_PATH/$ARTIFACT/$VERSION/$ARTIFACT-$VERSION.pom")
    }

    private fun pomContent(): String {
        assertTrue(pomFile.exists(), "Published POM not found at ${pomFile.absolutePath}. Run ./gradlew publishToMavenLocal first.")
        return pomFile.readText()
    }

    @Test
    fun `published pom declares N0 ocr contracts dependency`() {
        val pom = pomContent()
        assertTrue(
            pom.contains("education.cccp") && pom.contains("ocr-contracts"),
            "POM must declare education.cccp:ocr-contracts (N0 OCR boundary port, EPIC CDX-OCR-CONTRACTS US-3)."
        )
    }

    @Test
    fun `published pom no longer declares codex plugin dependency`() {
        val pom = pomContent()
        assertFalse(
            pom.contains("<artifactId>codex-plugin</artifactId>"),
            "POM must NOT declare codex-plugin anymore — N1->N2 inversion is dead (EPIC CDX-RAG-SOCLE RAG-4 FINAL, S-104/S-201)."
        )
    }
}
