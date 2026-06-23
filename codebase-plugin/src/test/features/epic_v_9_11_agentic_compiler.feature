@epic_v_9_11
Feature: AgenticCompiler — Executable artifacts from governance chunks
  As a codebase-gradle developer
  I want ontologized chunks to compile into directly executable artifacts
  So that governance rules block unsafe commands and procedures map to Gradle tasks

  @pre_hook
  Scenario: RULE INTERDIRE compiles into a PreHook that blocks git push
    Given a governance chunk of type "RULE" with verb "INTERDIRE" and content
      """
      INTERDICTION FORMELLE de git push sans permission explicite.
      """
    When I compile it into an executable artifact
    And I execute the artifact on tool "exec_shell" with command "git push origin main"
    Then the execution is blocked
    And the blocked reason mentions "git push"

  @pre_hook
  Scenario: RULE INTERDIRE with dry-run exception allows git push --dry-run
    Given a governance chunk of type "RULE" with verb "INTERDIRE" and content
      """
      INTERDICTION FORMELLE de git push sauf avec --dry-run.
      """
    When I compile it into an executable artifact
    And I execute the artifact on tool "exec_shell" with command "git push --dry-run origin main"
    Then the execution is allowed

  @gradle_task
  Scenario: PROCEDURE GENERER compiles into a Gradle task payload
    Given a governance chunk of type "PROCEDURE" with verb "GENERER" and content
      """
      . Generer le scenario pedagogique global
      """
    When I compile it into an executable artifact
    Then the executable artifact type is "GRADLE_TASK"
    And the Gradle task payload has task name "generateArtifact"

  @constraint
  Scenario: CONSTRAINT compiles into a constraint payload with bounds
    Given a governance chunk of type "CONSTRAINT" with verb "VALIDER" and content
      """
      Maximum 50k tokens EAGER (~3000 lignes)
      """
    When I compile it into an executable artifact
    Then the executable artifact type is "CONSTRAINT_CHECK"
    And the constraint payload has max tokens 50000 and max lines 3000

  @metadata
  Scenario: METADATA compiles into a metadata payload
    Given a governance chunk of type "METADATA" with verb "null" and content
      """
      :session: 147
      :date: 2026-06-23
      """
    When I compile it into an executable artifact
    Then the executable artifact type is "METADATA"
    And the metadata payload has key "session" and value "147"

  @prompt_template
  Scenario: CONCEPT INTERDIRE compiles into a prompt template payload
    Given a governance chunk of type "CONCEPT" with verb "INTERDIRE" and content
      """
      == Regles Absolues
      INTERDICTION FORMELLE de commit sans permission.
      """
    When I compile it into an executable artifact
    Then the executable artifact type is "PROMPT_TEMPLATE"
    And the prompt template payload contains "INTERDICTION"
