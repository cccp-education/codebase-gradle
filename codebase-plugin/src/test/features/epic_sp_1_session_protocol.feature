@epic_sp_1
Feature: Session Protocol — opencode↔Gradle thin client bridge
  As an opencode thin client
  I want to send a SessionPrompt to a Gradle task and receive a SessionResponse
  So that all LLM/RAG/tool logic runs in Gradle plugins, not in the CLI

  @protocol_basic
  Scenario: Send a prompt and receive a structured response
    Given a SessionProtocolTask is configured with FakeLlmProvider
    When I send prompt "Add dark mode toggle" with maxActions 3
    Then the response status is COMPLETED
    And the response contains "Add dark mode toggle"
    And the response has a valid sessionId

  @protocol_custom_id
  Scenario: Custom sessionId is preserved in response
    Given a SessionProtocolTask is configured with FakeLlmProvider
    When I send prompt "Test with custom ID" with sessionId "550e8400-e29b-41d4-a716-446655440000"
    Then the response sessionId is "550e8400-e29b-41d4-a716-446655440000"

  @protocol_token_tracking
  Scenario: Token usage is tracked in response
    Given a SessionProtocolTask is configured with FakeLlmProvider
    When I send prompt "Track my tokens" with model "gpt-oss:120b-cloud"
    Then the response contains tokenUsage
    And the response has non-zero promptTokens

  @protocol_error
  Scenario: LLM failure produces ERROR status
    Given a SessionProtocolTask is configured with ThrowingLlmProvider
    When I send prompt "This will fail" with maxActions 1
    Then the response status is ERROR

  @protocol_context
  Scenario: AgentContext is parsed from JSON file
    Given a SessionProtocolTask is configured with FakeLlmProvider
    And an AgentContext JSON file with eagerRules "No commits without permission"
    When I send prompt "Context-aware prompt" with contextFile
    Then the response status is COMPLETED
