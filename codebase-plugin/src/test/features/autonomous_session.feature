@autonomous-session
Feature: Autonomous vibecoding session orchestration
  As a codebase-gradle maintainer
  I want a Gradle task that chains augmented-context vibecoding sessions until the borough backlog is complete
  So that the resumption prompt is generated from the backlog instead of hand-written (EPIC SVO-4/5)

  Background:
    Given an autonomous session world is initialized

  Scenario: Transferred CLI prompt feeds the first vibecoding loop
    Given a borough dir without a backlog file
    When the autonomous session is orchestrated with transferred prompt "Work EPIC SVO-4 now"
    Then one session consumed the transferred prompt
    And the brainstorming session ran the intention "BRAINSTORM"
    And a resumption prompt was written under the vibecoding build dir

  Scenario: Blank CLI prompt falls back to the borough default prompt
    Given a borough dir with a PROMPT_REPRISE mentioning "Session 259"
    When the autonomous session is orchestrated without transferred prompt
    Then the first session consumed the borough default prompt
    And the borough default prompt carries the backlog open items

  Scenario: Open backlog keeps chaining sessions up to the bound
    Given a borough dir with a never-ending BACKLOG
    When the autonomous session is orchestrated without transferred prompt
    Then three chained sessions executed
    And the chain stopped with reason "maxChainedSessions"
    And the resumption prompt orients on the remaining backlog

  Scenario: Complete backlog triggers the brainstorming chain-up
    Given a borough dir whose BACKLOG is already complete
    When the autonomous session is orchestrated with transferred prompt "one shot"
    Then two sessions executed including the brainstorming chain-up
    And the brainstorming intention names the borough
