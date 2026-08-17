@vibe-hardening
Feature: Vibe Hardening — allowlist deny-by-default, LLM timeout, single retry counter
  As a codebase-gradle maintainer
  I want to harden the vibecoding execution layer against security bypasses and resilience gaps
  So that the autonomous agent can run unsupervised without hanging or over-retrying

  Background:
    Given a vibe hardening world is initialized

  Scenario: Shell allowlist denies non-whitelisted commands
    When the vibe hardening shell tool validates command "rm -rf /tmp"
    Then the vibe hardening shell tool rejects the command
    And the vibe hardening shell rejection message mentions "not in allowlist"

  Scenario: Gradle allowlist allows whitelisted tasks
    When the vibe hardening gradle tool validates task "compileKotlin"
    Then the vibe hardening gradle tool accepts the task

  Scenario: LLM call times out after llmTimeoutMs
    Given a slow LLM provider that sleeps for 10000 ms
    And a vibe hardening graph configured with llmTimeoutMs 50
    When I execute vibe hardening with intention "trigger timeout" and maxActions 3
    Then the vibe hardening result state is not null
    And the vibe hardening result has an error
    And the vibe hardening error contains "Timeout"

  Scenario: Retry counter is incremented once per retry
    Given a vibe hardening graph initialized with fake LLM for error recovery
    And the vibe hardening fake LLM suggests the next response "try again"
    When I execute vibe hardening with a 1-task failing plan and maxRetries 2
    Then the vibe hardening retry count is at most 2