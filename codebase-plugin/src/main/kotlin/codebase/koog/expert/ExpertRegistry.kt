package codebase.koog.expert

data class ExpertRegistration(
    val domain: ExpertDomain,
    val modelName: String,
    val baseUrl: String = "http://localhost:11437",
    val timeoutSeconds: Long = 120
)

class ExpertRegistry {
    private val experts = mutableMapOf<String, ExpertRegistration>()

    fun register(registration: ExpertRegistration) {
        experts[registration.domain.name] = registration
    }

    fun registerAll(registrations: List<ExpertRegistration>) {
        registrations.forEach { register(it) }
    }

    fun resolve(domain: ExpertDomain): ExpertRegistration? =
        experts[domain.name]

    fun resolveByName(domainName: String): ExpertRegistration? =
        experts[domainName]

    fun listDomains(): List<ExpertDomain> =
        experts.values.map { it.domain }

    fun size(): Int = experts.size

    fun clear() = experts.clear()
}
