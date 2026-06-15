@epic_sp_4
Feature: Session Protocol SP-4 — Tests E2E opencode→runner→codebase→session
  As a runner-gradle orchestrator
  I want to execute the full session lifecycle end-to-end
  So that opencode thin client can create, vibecode, and close sessions through Gradle tasks

  @e2e_create_vibecode_close
  Scenario: Full lifecycle — create session, vibecode, get response, close session
    Given an E2E SessionProtocolTask with lifecycle enabled
    When I E2E execute action "create" with prompt "Add dark mode toggle to settings" and model "deepseek-v4-pro"
    Then the E2E lifecycle shows 1 session with status RUNNING
    And the E2E session prompt is "Add dark mode toggle to settings"
    And the E2E session model is "deepseek-v4-pro"
    And the E2E session has a response with status COMPLETED
    And the E2E response contains tokenUsage
    When I E2E execute action "close" with sessionId of the created session
    Then the E2E created session has status CLOSED

  @e2e_create_resume_close
  Scenario: Full lifecycle — create, resume child, close parent
    Given an E2E SessionProtocolTask with lifecycle enabled
    When I E2E execute action "create" with prompt "Initial work session"
    Then the E2E lifecycle shows 1 session with status RUNNING
    When I E2E execute action "resume" with sessionId of the created session and prompt "Continue previous work"
    Then the E2E lifecycle shows a child of the created session with status RUNNING
    And the E2E child session has a response with status COMPLETED
    When I E2E execute action "close" with sessionId of the created session
    Then the E2E created session has status CLOSED

  @e2e_create_list_close
  Scenario: Full lifecycle — create, list, close, verify list reflects closure
    Given an E2E SessionProtocolTask with lifecycle enabled
    When I E2E execute action "create" with prompt "Session Alpha"
    Then the E2E lifecycle shows 1 session with status RUNNING
    When I E2E execute action "list"
    Then the E2E list response contains "Session Alpha"
    And the E2E list response contains "RUNNING"
    When I E2E execute action "close" with sessionId of the created session
    Then the E2E created session has status CLOSED
    When I E2E execute action "list"
    Then the E2E list response contains "Session Alpha"
    And the E2E list response contains "CLOSED"

  @e2e_create_with_context
  Scenario: Full lifecycle — create with AgentContext, vibecode, close
    Given an E2E SessionProtocolTask with lifecycle enabled
    And an E2E AgentContext JSON file with eagerRules "No commits without permission"
    When I E2E execute action "create" with prompt "Context-aware E2E task" and contextFile
    Then the E2E lifecycle shows 1 session with status RUNNING
    And the E2E session has a response with status COMPLETED
    When I E2E execute action "close" with sessionId of the created session
    Then the E2E created session has status CLOSED
