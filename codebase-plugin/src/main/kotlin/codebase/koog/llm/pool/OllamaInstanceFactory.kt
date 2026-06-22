package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance

/**
 * Factory déterministe de [LlmInstance] Ollama Cloud.
 *
 * Matérialise le pattern Docker du blog 0120 :
 * - chaque instance Ollama Pro vit dans son propre conteneur Docker
 * - port réseau distinct mappé sur l'hôte (11437 → 11465)
 * - volume SSH distinct monté dans `/root/.ollama` (identité device key)
 *
 * Le champ [LlmInstance.volumeTag] est un **tag métier** (ex: "ollama-11437")
 * qui identifie le volume Docker associé. Il ne contient jamais de clé SSH,
 * de chemin de fichier, ni de secret : l'auth est gérée côté conteneur.
 *
 * Modèles cyclés parmi les 5 modèles cloud autorisés par AGENT.adoc.
 */
object OllamaInstanceFactory {

    /** Plage par défaut des ports hôte mappés sur les conteneurs Ollama. */
    val DEFAULT_PORT_RANGE = 11437..11465

    /** 5 modèles cloud autorisés, cyclés sur les ports. */
    val AUTHORIZED_MODELS = listOf(
        "gpt-oss:120b-cloud",
        "gpt-oss:20b-cloud",
        "qwen3-coder-next:cloud",
        "qwen3-next:80b-cloud",
        "qwen3-coder:480b-cloud"
    )

    /**
     * Crée une liste d'instances Ollama Cloud, une par port.
     *
     * @param portRange plage de ports hôte (défaut 11437..11465)
     * @return liste ordonnée d'instances avec modèles cyclés
     */
    fun create(portRange: IntRange = DEFAULT_PORT_RANGE): List<LlmInstance> {
        require(portRange.first >= 1024 && portRange.last <= 65535) {
            "Port range must be within 1024..65535, got $portRange"
        }

        return portRange.mapIndexed { index, port ->
            val id = "ollama-$port"
            LlmInstance(
                id = id,
                baseUrl = "http://localhost:$port",
                model = AUTHORIZED_MODELS[index % AUTHORIZED_MODELS.size],
                volumeTag = id
            )
        }
    }
}
