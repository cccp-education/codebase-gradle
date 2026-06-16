package codebase.ocr

object TextAnonymizer {

    private val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
    private val phoneRegex = Regex("""(?:\+?\d{1,3}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{2,4}[-.\s]?\d{2,4}[-.\s]?\d{2,4}""")
    private val apiKeyRegex = Regex("""(?:sk|api|key|token|secret)[_-][a-zA-Z0-9_-]{15,}""")
    private val ibanRegex = Regex("""[A-Z]{2}\d{2}[A-Z0-9]{1,30}""")
    private val ssnRegex = Regex("""\d{1,2}\s?\d{2}\s?\d{2}\s?\d{2}\s?\d{3}\s?\d{3}\s?\d{2}""")

    fun anonymize(text: String): String {
        var result = text
        result = emailRegex.replace(result) { "***@anonymous.com" }
        result = phoneRegex.replace(result) { "***" }
        result = apiKeyRegex.replace(result) { "***" }
        result = ibanRegex.replace(result) { "***" }
        result = ssnRegex.replace(result) { "***" }
        return result
    }

    fun countReplacements(original: String, anonymized: String): Int {
        val emailCount = emailRegex.findAll(original).count()
        val phoneCount = phoneRegex.findAll(original).count()
        val apiKeyCount = apiKeyRegex.findAll(original).count()
        val ibanCount = ibanRegex.findAll(original).count()
        val ssnCount = ssnRegex.findAll(original).count()
        return emailCount + phoneCount + apiKeyCount + ibanCount + ssnCount
    }

    fun detectedCategories(text: String): List<String> {
        val categories = mutableListOf<String>()
        if (emailRegex.containsMatchIn(text)) categories.add("email")
        if (phoneRegex.containsMatchIn(text)) categories.add("phone")
        if (apiKeyRegex.containsMatchIn(text)) categories.add("api_key")
        if (ibanRegex.containsMatchIn(text)) categories.add("iban")
        if (ssnRegex.containsMatchIn(text)) categories.add("ssn")
        return categories
    }
}
