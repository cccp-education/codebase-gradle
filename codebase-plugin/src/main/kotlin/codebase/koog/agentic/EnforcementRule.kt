package codebase.koog.agentic

data class EnforcementRule(
    val sourceChunkId: String,
    val verb: TaxonomyVerb,
    val forbiddenPattern: String,
    val allowedPattern: String?,
    val toolName: String,
    val description: String
)

data class EnforcementResult(
    val allowed: Boolean,
    val blockedBy: String?,
    val reason: String?
) {
    companion object {
        fun allowed() = EnforcementResult(true, null, null)
        fun blocked(ruleId: String, reason: String) = EnforcementResult(false, ruleId, reason)
    }
}
