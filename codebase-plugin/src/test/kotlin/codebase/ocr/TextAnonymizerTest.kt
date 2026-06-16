package codebase.ocr

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextAnonymizerTest {

    @Test
    fun `anonymize replaces email addresses`() {
        val input = "Contact: jean.dupont@example.com pour info"
        val result = TextAnonymizer.anonymize(input)
        assertFalse(result.contains("jean.dupont@example.com"))
        assertTrue(result.contains("***@anonymous.com"))
    }

    @Test
    fun `anonymize replaces multiple emails`() {
        val input = "De: alice@acme.com À: bob@corp.org CC: carol@test.fr"
        val result = TextAnonymizer.anonymize(input)
        assertFalse(result.contains("@acme.com"))
        assertFalse(result.contains("@corp.org"))
        assertFalse(result.contains("@test.fr"))
        assertEquals(3, Regex.escape("***@anonymous.com").toRegex().findAll(result).count())
    }

    @Test
    fun `anonymize replaces phone numbers`() {
        val input = "Tel: +33 6 12 34 56 78 ou 01 23 45 67 89"
        val result = TextAnonymizer.anonymize(input)
        assertFalse(result.contains("+33 6 12 34 56 78"))
        assertFalse(result.contains("01 23 45 67 89"))
    }

    @Test
    fun `anonymize replaces API key patterns`() {
        val input = "Authorization: sk-ant-api03-abcdefghijklmnopqrstuvwxyz123456"
        val result = TextAnonymizer.anonymize(input)
        assertFalse(result.contains("sk-ant-api03"))
        assertTrue(result.contains("***"))
    }

    @Test
    fun `anonymize replaces IBAN`() {
        val input = "Virement vers FR7630001007941234567890185"
        val result = TextAnonymizer.anonymize(input)
        assertFalse(result.contains("FR7630001007941234567890185"))
    }

    @Test
    fun `anonymize replaces SSN`() {
        val input = "N° sécurité sociale : 1 85 12 75 123 456 78"
        val result = TextAnonymizer.anonymize(input)
        assertFalse(result.contains("1 85 12 75 123 456 78"))
    }

    @Test
    fun `anonymize preserves non-sensitive text`() {
        val input = "= Document OCRisé\n\n== Introduction\n\nCeci est un document de test."
        val result = TextAnonymizer.anonymize(input)
        assertEquals(input, result)
    }

    @Test
    fun `anonymize handles empty text`() {
        val result = TextAnonymizer.anonymize("")
        assertEquals("", result)
    }

    @Test
    fun `anonymize handles text with no PII`() {
        val input = "Bonjour, voici le rapport trimestriel. Cordialement, L'équipe."
        val result = TextAnonymizer.anonymize(input)
        assertEquals(input, result)
    }

    @Test
    fun `countReplacements returns correct count`() {
        val input = "Email: test@test.com, Tel: 06 12 34 56 78, Key: sk-abc123def456ghi789jkl012mno345pqr678"
        val anonymized = TextAnonymizer.anonymize(input)
        val count = TextAnonymizer.countReplacements(input, anonymized)
        assertEquals(3, count)
    }

    @Test
    fun `countReplacements returns zero for clean text`() {
        val input = "Rien de sensible ici."
        val anonymized = TextAnonymizer.anonymize(input)
        val count = TextAnonymizer.countReplacements(input, anonymized)
        assertEquals(0, count)
    }

    @Test
    fun `detectedCategories returns email when email present`() {
        val input = "Contact: info@example.com"
        val categories = TextAnonymizer.detectedCategories(input)
        assertTrue(categories.contains("email"))
    }

    @Test
    fun `detectedCategories returns phone when phone present`() {
        val input = "Appeler le 06 12 34 56 78"
        val categories = TextAnonymizer.detectedCategories(input)
        assertTrue(categories.contains("phone"))
    }

    @Test
    fun `detectedCategories returns api_key when API key present`() {
        val input = "Authorization: sk-ant-api03-abcdefghijklmnopqrstuvwxyz123456"
        val categories = TextAnonymizer.detectedCategories(input)
        assertTrue(categories.contains("api_key"))
    }

    @Test
    fun `detectedCategories returns iban when IBAN present`() {
        val input = "IBAN: FR7630001007941234567890185"
        val categories = TextAnonymizer.detectedCategories(input)
        assertTrue(categories.contains("iban"))
    }

    @Test
    fun `detectedCategories returns ssn when SSN present`() {
        val input = "SSN: 1 85 12 75 123 456 78"
        val categories = TextAnonymizer.detectedCategories(input)
        assertTrue(categories.contains("ssn"))
    }

    @Test
    fun `detectedCategories returns empty for clean text`() {
        val input = "Aucune donnée personnelle."
        val categories = TextAnonymizer.detectedCategories(input)
        assertTrue(categories.isEmpty())
    }

    @Test
    fun `anonymize is idempotent`() {
        val input = "Email: test@test.com, Key: sk-abc123"
        val first = TextAnonymizer.anonymize(input)
        val second = TextAnonymizer.anonymize(first)
        assertEquals(first, second)
    }
}
