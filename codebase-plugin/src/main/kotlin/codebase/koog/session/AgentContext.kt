package codebase.koog.session

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class AgentContext(
    val eagerRules: String = "",
    val backlogItems: List<@Contextual String> = emptyList(),
    val graphRelations: String = "",
    val ragChunks: List<@Contextual String> = emptyList()
)
