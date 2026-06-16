package codebase.koog.expert

data class ExpertExposureEntry(
    val domain: ExpertDomain,
    val modelName: String,
    val baseUrl: String,
    val timeoutSeconds: Long
) {
    companion object {
        fun from(registration: ExpertRegistration): ExpertExposureEntry =
            ExpertExposureEntry(
                domain = registration.domain,
                modelName = registration.modelName,
                baseUrl = registration.baseUrl,
                timeoutSeconds = registration.timeoutSeconds
            )
    }

    fun anonymize(): ExpertExposureEntry =
        copy(baseUrl = "***anonymized***")
}

data class ExpertExposureManifest(
    val version: String,
    val generatedAt: String,
    val experts: List<ExpertExposureEntry>
) {
    fun anonymize(): ExpertExposureManifest =
        copy(experts = experts.map { it.anonymize() })
}

data class ExpertExposureConfig(
    val domains: List<String> = emptyList(),
    val outputFormat: String = "json",
    val anonymizeEndpoints: Boolean = true,
    val outputFile: String = "build/experts/exposure-manifest.json"
)
