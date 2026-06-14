@epic_x_1
Feature: VibecodingPlan — Multi-step structured plan
  As a vibecoding agent
  I want a structured multi-step plan with verification and rollback
  So that I can execute, verify, and adapt across multiple steps

  @plan_construction
  Scenario: Build a plan with ordered steps
    Given an empty VibecodingPlan builder
    When I add step "compile" with gradle task "build" and expected output "BUILD SUCCESSFUL"
    And I add step "test" with gradle task "test" and expected output "All tests passed"
    And I add step "publish" with gradle task "publish" and expected output "BUILD SUCCESSFUL"
    Then the plan has 3 steps
    And step 1 has description "compile"
    And step 2 has description "test"
    And step 3 has description "publish"

  @plan_strategy
  Scenario: Plan with REVERT_AND_CONTINUE strategy
    Given an empty VibecodingPlan builder
    When I add step "compile" with gradle task "build" and expected output "BUILD SUCCESSFUL"
    And I set rollback strategy to REVERT_AND_CONTINUE
    Then the plan has strategy REVERT_AND_CONTINUE

  @plan_strategy_mark_skipped
  Scenario: Plan with MARK_SKIPPED strategy
    Given an empty VibecodingPlan builder
    When I add step "compile" with gradle task "build" and expected output "BUILD SUCCESSFUL"
    And I set rollback strategy to MARK_SKIPPED
    Then the plan has strategy MARK_SKIPPED

  @plan_strategy_fallback
  Scenario: Plan with FALLBACK_HUMAN strategy
    Given an empty VibecodingPlan builder
    When I add step "compile" with gradle task "build" and expected output "BUILD SUCCESSFUL"
    And I set rollback strategy to FALLBACK_HUMAN
    Then the plan has strategy FALLBACK_HUMAN

  @plan_defaults
  Scenario: Default strategy is STOP_ON_ERROR
    Given an empty VibecodingPlan builder
    When I add step "compile" with gradle task "build" and expected output "BUILD SUCCESSFUL"
    Then the plan has strategy STOP_ON_ERROR

  @step_retries
  Scenario: Step with custom maxRetries
    Given an empty VibecodingPlan builder
    When I add step "fragile" with gradle task "fragileTask" expected output "OK" and maxRetries 5
    Then step "fragile" has maxRetries 5

  @step_verify_hook
  Scenario: Step with verifyHook
    Given an empty VibecodingPlan builder
    When I add step "test" with gradle task "test" expected output "All tests passed" and verifyHook "grep FAILED"
    Then step "test" has verifyHook "grep FAILED"

  @step_defaults
  Scenario: Step defaults are 3 retries and no verifyHook
    Given an empty VibecodingPlan builder
    When I add step "compile" with gradle task "build" and expected output "BUILD SUCCESSFUL"
    Then step "compile" has maxRetries 3
    And step "compile" has no verifyHook
