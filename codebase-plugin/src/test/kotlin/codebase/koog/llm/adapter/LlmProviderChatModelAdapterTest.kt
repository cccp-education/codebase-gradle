package codebase.koog.llm.adapter

import codebase.koog.llm.LlmProvider
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.request.ChatRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LlmProviderChatModelAdapterTest {

    @Test
    fun `doChat forwards a single user message to the provider and wraps the response`() {
        val provider = LlmProvider { prompt ->
            assertThat(prompt).isEqualTo("Hello, LLM!")
            "LLM reply"
        }
        val adapter = LlmProviderChatModelAdapter(provider)
        val request = ChatRequest.builder()
            .messages(UserMessage.from("Hello, LLM!"))
            .build()
        val response = adapter.doChat(request)
        assertThat(response.aiMessage().text()).isEqualTo("LLM reply")
    }

    @Test
    fun `doChat concatenates system and user messages with newline`() {
        val provider = LlmProvider { prompt ->
            assertThat(prompt).contains("You are a planner.\nDecompose this intention")
            "plan"
        }
        val adapter = LlmProviderChatModelAdapter(provider)
        val request = ChatRequest.builder()
            .messages(
                SystemMessage.from("You are a planner."),
                UserMessage.from("Decompose this intention"),
            )
            .build()
        val response = adapter.doChat(request)
        assertThat(response.aiMessage().text()).isEqualTo("plan")
    }

    @Test
    fun `doChat returns an AiMessage wrapping the raw provider output`() {
        val provider = LlmProvider { "raw output" }
        val adapter = LlmProviderChatModelAdapter(provider)
        val request = ChatRequest.builder()
            .messages(UserMessage.from("anything"))
            .build()
        val response = adapter.doChat(request)
        assertThat(response.aiMessage()).isNotNull
        assertThat(response.aiMessage().text()).isEqualTo("raw output")
    }

    @Test
    fun `doChat with a single-space message produces a single-space prompt`() {
        val captured = mutableListOf<String>()
        val provider = LlmProvider { prompt ->
            captured.add(prompt)
            "ok"
        }
        val adapter = LlmProviderChatModelAdapter(provider)
        val request = ChatRequest.builder()
            .messages(UserMessage.from(" "))
            .build()
        adapter.doChat(request)
        assertThat(captured).hasSize(1)
        assertThat(captured.first()).isEqualTo(" ")
    }

    @Test
    fun `doChat propagates provider exceptions`() {
        val provider = LlmProvider { throw RuntimeException("provider down") }
        val adapter = LlmProviderChatModelAdapter(provider)
        val request = ChatRequest.builder()
            .messages(UserMessage.from("hello"))
            .build()
        assertThrows<RuntimeException> { adapter.doChat(request) }
    }
}