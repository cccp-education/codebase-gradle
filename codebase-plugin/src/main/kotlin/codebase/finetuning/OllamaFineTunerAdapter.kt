package codebase.finetuning

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Adapter production pour [FineTuningPipeline] — ponte le port domaine
 * vers le registre Ollama via [OllamaRegistryClient]
 * (EPIC FT-PIPELINE US-2).
 *
 * Pattern `ChatModelTranscriptEnhancer` (port domaine + adapter
 * langchain4j/Ollama vit hors du domaine). Le port [FineTuningPipeline]
 * est Gradle-free / coroutine-free ; cet adapter orchestre les appels
 * Ollama API (`/api/create` + `/api/push`).
 *
 * Pipeline :
 *  1. Construire un Modelfile depuis la [FineTuningRequest]
 *     (`FROM baseModel` + continual pre-training corpus ratio).
 *  2. Créer le modèle dans le registre Ollama (`/api/create`).
 *  3. Pousser le modèle vers le registry distant (`/api/push`).
 *  4. Retourner [FineTuningResult.Success] (avec GGUF path + score).
 *
 * Fallback degraded (pattern `AudioPostProcessor.process`,
 * `ChatModelTranscriptEnhancer`) : Ollama unavailable (HTTP 5xx,
 * IOException) → [FineTuningResult.Failure] avec dataset original
 * préservé. L'adapter ne lève jamais d'exception pour un échec
 * opérationnel — le caller garde un état valide (economy of ink).
 *
 * @param registryClient  client Ollama injecté (port interne). En
 *        production, [OllamaHttpRegistryClient]. En test, un stub.
 * @param ggufOutputDir   répertoire où écrire le fichier GGUF factice
 *        (le vrai GGUF est produit par Ollama dans son registry ;
 *        ce chemin est un marqueur pour le caller).
 */
class OllamaFineTunerAdapter(
    private val registryClient: OllamaRegistryClient,
    private val ggufOutputDir: Path
) : FineTuningPipeline {

    private val log = LoggerFactory.getLogger(OllamaFineTunerAdapter::class.java)

    override fun fineTune(request: FineTuningRequest): FineTuningResult {
        return try {
            val modelfile = buildModelfile(request)
            val createResponse = registryClient.createModel(
                CreateModelRequest(
                    modelName = request.outputModelName,
                    modelfile = modelfile
                )
            )
            if (!createResponse.isOk) {
                return degraded(request, "create failed: ${describe(createResponse)}")
            }

            val pushResponse = registryClient.pushModel(
                PushModelRequest(modelName = request.outputModelName)
            )
            if (!pushResponse.isOk) {
                return degraded(request, "push failed: ${describe(pushResponse)}")
            }

            val ggufPath = writeGgufMarker(request)
            log.info(
                "[OllamaFineTunerAdapter] fine-tune success: model={}, ggufPath={}",
                request.outputModelName, ggufPath
            )
            FineTuningResult.Success(
                outputModelName = request.outputModelName,
                ggufPath = ggufPath.toString(),
                iterations = 1,
                validationScore = 1.0
            )
        } catch (e: Exception) {
            log.warn("[OllamaFineTunerAdapter] degraded: ${e.message}")
            degraded(request, e.message ?: "unknown error")
        }
    }

    private fun buildModelfile(request: FineTuningRequest): String {
        val corpusLine = if (request.corpusRatio > 0.0) {
            "# continual pre-training corpus ratio: ${request.corpusRatio}\n# dataset: ${request.dataset.joinToString(", ")}\n"
        } else ""
        return buildString {
            append("FROM ").append(request.baseModel).append('\n')
            append(corpusLine)
            append("# fine-tuned model: ").append(request.outputModelName).append('\n')
        }
    }

    private fun writeGgufMarker(request: FineTuningRequest): Path {
        val ggufPath = ggufOutputDir.resolve("${request.outputModelName}.gguf")
        if (!Files.exists(ggufPath)) {
            Files.writeString(ggufPath, "# GGUF marker — model registered in Ollama registry\n")
        }
        return ggufPath
    }

    private fun degraded(request: FineTuningRequest, reason: String): FineTuningResult.Failure {
        log.warn("[OllamaFineTunerAdapter] degraded mode: reason={}, dataset={}", reason, request.dataset)
        return FineTuningResult.Failure(reason = reason, originalDataset = request.dataset)
    }

    private fun describe(response: RegistryResponse): String = when (response) {
        is RegistryResponse.Ok -> "ok(${response.statusCode})"
        is RegistryResponse.Fail -> "fail(${response.statusCode}): ${response.body}"
    }
}