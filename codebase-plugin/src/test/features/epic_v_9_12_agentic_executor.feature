@epic_v_9_12
Feature: AgenticExecutor — Governance rules enforce tool calls
  As a codebase-gradle developer
  I want the ingestion task to produce an executor that enforces governance rules
  So that unsafe exec_shell and exec_gradle calls are blocked by ingested chunks

  @pre_hook
  Scenario: Ingested RULE INTERDIRE blocks git push via exec_shell
    Given a governance file "AGENT.adoc" with content
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de git push sans permission explicite.
      """
    When I ingest the governance file
    Then the ingestion report has executables
    And the executor blocks tool "exec_shell" with command "git push origin main"

  @pre_hook
  Scenario: Ingested RULE INTERDIRE allows git push with dry-run exception
    Given a governance file "AGENT.adoc" with content
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de git push sauf avec --dry-run.
      """
    When I ingest the governance file
    Then the executor allows tool "exec_shell" with command "git push --dry-run origin main"

  @gradle_task
  Scenario: Ingested RULE INTERDIRE blocks gradle publish via exec_gradle
    Given a governance file "AGENT.adoc" with content
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de ./gradlew publish sans verification.
      """
    When I ingest the governance file
    Then the executor blocks tool "exec_gradle" with task "publish"

  @multi_rule
  Scenario: Ingested multiple rules block matching commands
    Given a governance file "AGENT.adoc" with content
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de commit sans permission explicite.
      **INTERDICTION FORMELLE** de merge sans permission explicite.
      **INTERDICTION FORMELLE** de git push sans flag --dry-run.
      """
    When I ingest the governance file
    Then the executor blocks tool "exec_shell" with command "git commit -m 'test'"
    And the executor blocks tool "exec_shell" with command "git merge feature-branch"
    And the executor blocks tool "exec_shell" with command "git push origin main"
    And the executor allows tool "exec_shell" with command "git push --dry-run origin main"

  @unrelated
  Scenario: Ingested rules do not block unrelated tools
    Given a governance file "AGENT.adoc" with content
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de git push sans permission explicite.
      """
    When I ingest the governance file
    Then the executor allows tool "exec_gradle" with task "build"
    And the executor allows tool "exec_shell" with command "ls -la"
