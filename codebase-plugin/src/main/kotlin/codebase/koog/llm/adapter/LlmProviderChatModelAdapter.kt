package codebase.koog.llm.adapter

import codebase.koog.llm.LlmProvider
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import kotlinx.coroutines.runBlocking

/**
 * Adapter bridging codebase's N1 [LlmProvider] (koog abstraction, suspend)
 * to langchain4j's [ChatModel] (cross-borough N2 consumption contract).
 *
 * Architecture (EPIC LLM-ADAPTER-EXTRACT): koog orchestrates, langchain4j
 * executes. The 3 LLM-consuming boroughs (slider, capsule, planner) all
 * consume `ChatModel.chat(messages).aiMessage().text()` — this adapter
 * preserves that signature while delegating the actual LLM call to
 * codebase's [LlmProvider] via [runBlocking].
 *
 * Messages are concatenated into a single prompt string (system first, then
 * user), matching [LlmProvider]'s single-string contract. The response is
 * wrapped in an [AiMessage] inside a [ChatResponse].
 *
 * @param provider  the codebase N1 LLM provider (typically resolved via
 *                  `codebase.koog.llm.service.LlmBuildService`).
 */
class LlmProviderChatModelAdapter(
    private val provider: LlmProvider,
) : ChatModel {

    override fun doChat(request: ChatRequest): ChatResponse {
        val prompt = request.messages().joinToString("\n") { it.text() }
        val raw = runBlocking { provider.call(prompt) }
        return ChatResponse.builder()
            .aiMessage(AiMessage.from(raw))
            .build()
    }

    /** ChatMessage.text() is not on the interface; resolve per concrete subtype. */
    private fun ChatMessage.text(): String = when (this) {
        is SystemMessage -> text()
        is UserMessage -> if (hasSingleText()) singleText() else contents().joinToString { it.toString() }
        is AiMessage -> text()
        else -> toString()
    }
}