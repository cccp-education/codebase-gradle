@epic_x_2
Feature: TaskResultVerifier — Parse Gradle output into verdicts
  As a vibecoding agent
  I want to parse Gradle task output into structured verdicts
  So that I can decide whether to continue, retry, or rollback

  @verdict_success
  Scenario: BUILD SUCCESSFUL returns SUCCESS
    Given a TaskResultVerifier
    When I verify stdout "BUILD SUCCESSFUL in 5s" and stderr ""
    Then the verdict is SUCCESS
    And the error message is empty

  @verdict_failed
  Scenario: BUILD FAILED returns FAILED
    Given a TaskResultVerifier
    When I verify stdout "BUILD FAILED in 2s" and stderr "compilation error"
    Then the verdict is FAILED
    And the error message contains "compilation error"

  @verdict_blocked
  Scenario: Missing task returns BLOCKED
    Given a TaskResultVerifier
    When I verify stdout "" and stderr "Task 'generateSPD' not found in project"
    Then the verdict is BLOCKED
    And the error message contains "not found"

  @verdict_unknown
  Scenario: Unknown output returns UNKNOWN
    Given a TaskResultVerifier
    When I verify stdout "Some random output" and stderr ""
    Then the verdict is UNKNOWN

  @verdict_tests_failed
  Scenario: Tests failed returns FAILED
    Given a TaskResultVerifier
    When I verify stdout "BUILD FAILED\n3 tests failed" and stderr "FooTest: assertion error"
    Then the verdict is FAILED

  @verdict_empty
  Scenario: Empty output returns UNKNOWN
    Given a TaskResultVerifier
    When I verify stdout "" and stderr ""
    Then the verdict is UNKNOWN
