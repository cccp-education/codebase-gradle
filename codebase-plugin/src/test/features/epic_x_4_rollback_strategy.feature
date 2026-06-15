@epic_x_4
Feature: RollbackStrategy — Recovery strategies on maxRetries exhausted
  As a vibecoding agent
  I want structured rollback strategies when maxRetries are exhausted
  So that I can recover gracefully instead of just failing

  @rollback_stop_on_error
  Scenario: STOP_ON_ERROR marks session finished with error
    Given a RollbackStrategyExecutor with workspace "/tmp/test"
    And a VibecodingState with retryCount 3 and maxRetries 3
    And a VibecodingPlan with strategy STOP_ON_ERROR and step "compile" task "build"
    When the executor executes the rollback
    Then the state is finished
    And the error contains "STOP_ON_ERROR"
    And the error contains "compile"

  @rollback_revert_and_continue
  Scenario: REVERT_AND_CONTINUE reverts files and continues
    Given a RollbackStrategyExecutor with workspace "/tmp/test"
    And a VibecodingState with retryCount 3 and maxRetries 3
    And a VibecodingPlan with strategy REVERT_AND_CONTINUE and step "compile" task "build"
    And modified files "src/main/Foo.kt"
    When the executor executes the rollback
    Then the state is not finished
    And the error is cleared
    And the retryCount is reset to 0

  @rollback_mark_skipped
  Scenario: MARK_SKIPPED marks step skipped and continues
    Given a RollbackStrategyExecutor with workspace "/tmp/test"
    And a VibecodingState with retryCount 3 and maxRetries 3
    And a VibecodingPlan with strategy MARK_SKIPPED and step "compile" task "build"
    When the executor executes the rollback
    Then the state is not finished
    And the error is cleared
    And the retryCount is reset to 0
    And the lastToolResult contains "SKIPPED"

  @rollback_fallback_human
  Scenario: FALLBACK_HUMAN pauses and requests human input
    Given a RollbackStrategyExecutor with workspace "/tmp/test"
    And a VibecodingState with retryCount 3 and maxRetries 3
    And a VibecodingPlan with strategy FALLBACK_HUMAN and step "compile" task "build"
    When the executor executes the rollback
    Then the state is finished
    And the error contains "FALLBACK_HUMAN"
    And the error contains "compile"

  @rollback_default_stop
  Scenario: Default strategy is STOP_ON_ERROR when no executor
    Given a RollbackStrategyExecutor with workspace "/tmp/test"
    And a VibecodingState with retryCount 3 and maxRetries 3
    And a VibecodingPlan with strategy STOP_ON_ERROR and step "compile" task "build"
    When the executor executes the rollback
    Then the state is finished
    And the error contains "STOP_ON_ERROR"
