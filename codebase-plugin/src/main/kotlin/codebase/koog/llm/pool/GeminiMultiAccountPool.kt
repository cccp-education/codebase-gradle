package codebase.koog.llm.pool

import contracts.llmpool.LlmInstance
import contracts.llmpool.RotationStrategy

/**
 * Pool multi-comptes Gemini : 1 [GeminiKeyPool] par compte Google.
 *
 * Chaque compte Google possède N clés API. Ce pool gère la rotation
 * entre les comptes (round-robin sur les sous-pools) et à l'intérieur
 * de chaque compte (rotation du [GeminiKeyPool] sous-jacent).
 *
 * Variables d'environnement attendues :
 * - `GEMINI_ACCOUNT_{accountId}_API_KEY_{keyIndex}` (ex: GEMINI_ACCOUNT_1_API_KEY_1)
 *
 * @param accountPools map accountId → GeminiKeyPool
 */
class GeminiMultiAccountPool(
    private val accountPools: Map<String, GeminiKeyPool>
) {

    private val accountIds = accountPools.keys.toList()
    private var currentAccountIndex = 0

    fun accountCount(): Int = accountPools.size

    fun totalSize(): Int = accountPools.values.sumOf { it.size() }

    fun nextInstance(): LlmInstance {
        if (accountPools.isEmpty()) {
            throw IllegalStateException("Gemini multi-account pool is empty — no accounts configured")
        }
        val accountId = accountIds[currentAccountIndex % accountIds.size]
        val instance = accountPools[accountId]!!.nextInstance()
        currentAccountIndex = (currentAccountIndex + 1) % accountIds.size
        return instance
    }

    fun markRateLimited(instance: LlmInstance) {
        for (pool in accountPools.values) {
            if (pool.instances().any { it.id == instance.id }) {
                pool.markRateLimited(instance)
                return
            }
        }
    }

    fun isRateLimited(instance: LlmInstance): Boolean {
        return accountPools.values.any { pool ->
            pool.instances().any { it.id == instance.id } && pool.isRateLimited(instance)
        }
    }

    fun resetUsage() {
        accountPools.values.forEach { it.resetUsage() }
        currentAccountIndex = 0
    }

    companion object {
        private const val ENV_PREFIX = "GEMINI_ACCOUNT_"
        private const val ENV_KEY_SUFFIX = "_API_KEY_"
        private const val MAX_ACCOUNT_INDEX = 50
        private const val MAX_KEY_INDEX = 100

        fun fromEnvVars(
            env: Map<String, String>,
            model: String = "gemini-2.5-flash"
        ): GeminiMultiAccountPool {
            val accounts = mutableMapOf<String, GeminiKeyPool>()
            for (accountId in 1..MAX_ACCOUNT_INDEX) {
                val keys = (1..MAX_KEY_INDEX).mapNotNull { keyIndex ->
                    env["${ENV_PREFIX}${accountId}${ENV_KEY_SUFFIX}${keyIndex}"]
                        ?.takeIf { it.isNotBlank() }
                }
                if (keys.isNotEmpty()) {
                    accounts["account-$accountId"] =
                        GeminiPoolFactory.fromKeys(keys, model = model, idPrefix = "gemini-acct$accountId")
                }
            }
            return GeminiMultiAccountPool(accounts)
        }
    }
}