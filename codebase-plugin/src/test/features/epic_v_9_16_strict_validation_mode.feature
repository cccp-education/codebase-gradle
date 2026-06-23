@epic_v_9_16
Feature: Strict validation mode for governance ingestion
  As a codebase-gradle developer
  I want IngestGovernanceTask to fail when strictValidation is enabled and invalid chunks are found
  So that CI can block governance corruption before it reaches the agent context

  @strict_validation @failure
  Scenario: Strict validation fails on invalid chunks
    Given a quarantine test project with file "AGENT.adoc" containing
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    And a chunk validator that rejects every chunk with checksum mismatch
    When I run IngestGovernanceTask with strict validation enabled
    Then the task should fail with a strict validation error
    And the error should mention the quarantined chunk error type

  @strict_validation @success
  Scenario: Strict validation passes when all chunks are valid
    Given a quarantine test project with file "AGENT.adoc" containing
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run IngestGovernanceTask with strict validation enabled
    Then the task should succeed
    And the ingestion report should contain no quarantined chunks

  @strict_validation @default
  Scenario: Strict validation is disabled by default
    Given a quarantine test project with file "AGENT.adoc" containing
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    And a chunk validator that rejects every chunk with checksum mismatch
    When I run IngestGovernanceTask for quarantine on the project
    Then the task should succeed
    And the ingestion report should contain quarantined chunks
