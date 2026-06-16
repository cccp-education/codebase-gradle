package codebase.scenarios

import codebase.koog.expert.ExpertCallPipeline
import codebase.koog.expert.ExpertCallRepository
import codebase.koog.expert.ExpertDomain
import codebase.koog.expert.ExpertRegistration
import codebase.koog.expert.ExpertRegistry
import codebase.koog.expert.DispatcherAgent
import codebase.koog.expert.FakeExpertAgent
import codebase.koog.llm.FakeLlmProvider

class Epic7ExpertWorld {
    val registry = ExpertRegistry()
    val fakeDispatcherLlm = FakeLlmProvider()
    val fakeKotlinAgent = FakeExpertAgent(ExpertDomain("kotlin", "Kotlin, Gradle, JVM ecosystem"), "Kotlin expert output for kotlin")
    val fakeDocsAgent = FakeExpertAgent(ExpertDomain("docs", "Documentation, AsciiDoc, technical writing"), "Docs expert output for docs")

    lateinit var dispatcher: DispatcherAgent
    lateinit var pipeline: ExpertCallPipeline
    lateinit var repository: ExpertCallRepository
    lateinit var lastResult: DispatcherAgent.DispatcherResult
    lateinit var lastPipelineResult: ExpertCallPipeline.ExpertCallPipelineResult

    var lastPrompt: String = ""
    var lastAnonymizedPrompt: String = ""
}
