Feature: EPIC V-9.18 Compile governance chunks to Gradle tasks

  As a workspace maintainer
  I want PROCEDURE and CONSTRAINT chunks to produce real Gradle tasks
  So that governance rules become executable in the build

  @epic_v_9_18
  Scenario: GENERER procedure chunk registers a runProcedure task
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      == Generate Step
      . GENERER le rapport pedagogique
      """
    When I run the ingestGovernance task on the project
    Then the ingestion report has artifacts compiled greater than 0
    And a Gradle task named like "runProcedure_*" is registered

  @epic_v_9_18
  Scenario: CONSTRAINT chunk registers an enforceRule task
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      == Limits
      Maximum 50k tokens EAGER (~3000 lignes).
      """
    When I run the ingestGovernance task on the project
    Then a Gradle task named like "enforceRule_*" is registered

  @epic_v_9_18
  Scenario: Registered governance task is executable
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      == Check Step
      . GENERER le bilan
      """
    When I run the ingestGovernance task on the project
    Then the registered governance task can be executed
