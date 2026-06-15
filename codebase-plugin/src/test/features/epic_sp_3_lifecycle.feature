@epic_sp_3
Feature: Session Protocol SP-3 — Cycle de vie session (création, reprise, clôture)
  As a runner-gradle orchestrator
  I want to manage session lifecycle via SessionProtocolTask
  So that opencode sessions can be created, resumed, and closed through Gradle tasks

  @lifecycle_create
  Scenario: Create a session with lifecycle tracking
    Given a SessionProtocolTask with lifecycle enabled
    When I execute action "create" with prompt "Fix typo in README"
    Then the lifecycle shows 1 session with status RUNNING
    And the session prompt is "Fix typo in README"

  @lifecycle_create_custom_id
  Scenario: Create a session with custom sessionId
    Given a SessionProtocolTask with lifecycle enabled
    When I execute action "create" with prompt "Custom ID test" and sessionId "550e8400-e29b-41d4-a716-446655440000"
    Then the lifecycle session "550e8400-e29b-41d4-a716-446655440000" has prompt "Custom ID test"

  @lifecycle_resume
  Scenario: Resume a session creates child with parent reference
    Given a SessionProtocolTask with lifecycle enabled
    And a session "parent-123" exists with prompt "Initial prompt"
    When I execute action "resume" with sessionId "parent-123" and prompt "Continue work"
    Then the lifecycle shows a child of "parent-123" with status RUNNING

  @lifecycle_close
  Scenario: Close a session marks it as CLOSED
    Given a SessionProtocolTask with lifecycle enabled
    And a session "session-to-close" exists with prompt "Work in progress"
    When I execute action "close" with sessionId "session-to-close"
    Then the lifecycle session "session-to-close" has status CLOSED

  @lifecycle_list
  Scenario: List sessions shows all tracked sessions
    Given a SessionProtocolTask with lifecycle enabled
    And a session "list-a" exists with prompt "Session Alpha"
    And a session "list-b" exists with prompt "Session Beta"
    When I execute action "list"
    Then the list response contains "Session Alpha"
    And the list response contains "Session Beta"

  @lifecycle_create_persistence
  Scenario: Created sessions survive task recreation
    Given a SessionProtocolTask with lifecycle enabled in persistent directory
    When I execute action "create" with prompt "Persistent session"
    And I recreate the lifecycle manager using the same directory
    Then the lifecycle shows 1 session with prompt "Persistent session"
