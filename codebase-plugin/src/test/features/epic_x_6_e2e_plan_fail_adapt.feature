@epic_x_6
Feature: End-to-End — Agent plans, fails, adapts, succeeds
  As a vibecoding agent
  I want to plan multi-step tasks, handle failures, adapt, and succeed
  So that I can autonomously deliver features end-to-end

  @e2e_plan_fail_adapt_succeed
  Scenario: Agent plans compile→test→publish, compile fails once, replans, continues to success
    Given a VibecodingGraph with FakeLlmProvider and RollbackStrategyExecutor
    And a VibecodingState with plan "compile→test→publish" and maxRetries 3
    And the first compile will fail with "compilation error in Foo.kt"
    And the LLM will suggest "fix typo and recompile"
    And the next task will succeed
    And the next task will succeed
    When the agent executes the full pipeline
    Then all 3 steps are executed
    And the final state is finished
    And the final error is null

  @e2e_max_retries_exhausted_rollback
  Scenario: Agent exhausts maxRetries and triggers STOP_ON_ERROR rollback
    Given a VibecodingGraph with FakeLlmProvider and RollbackStrategyExecutor
    And a VibecodingState with plan "compile" and maxRetries 1 and rollbackStrategy STOP_ON_ERROR
    And the compile will always fail with "compilation error"
    And the LLM will suggest "try again"
    When the agent executes the full pipeline
    Then the final state is finished
    And the final error contains "STOP_ON_ERROR"

  @e2e_mark_skipped_continue
  Scenario: Agent exhausts maxRetries and triggers MARK_SKIPPED rollback
    Given a VibecodingGraph with FakeLlmProvider and RollbackStrategyExecutor
    And a VibecodingState with plan "compile" and maxRetries 1 and rollbackStrategy MARK_SKIPPED
    And the compile will always fail with "compilation error"
    And the LLM will suggest "try again"
    When the agent executes the full pipeline
    Then the final state is finished
    And the final error is null
    And all 1 steps are executed
