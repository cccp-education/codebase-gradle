@epic_v_9_13
Feature: ToolRegistry enforcement hook — Ingested governance blocks dangerous tools
  As a codebase-gradle developer
  I want ToolRegistry.execute() to call AgenticExecutor.check() as a pre-execution hook
  So that exec_shell and exec_gradle are blocked natively before any real execution

  @pre_hook
  Scenario: Ingested RULE INTERDIRE blocks git push via exec_shell in ToolRegistry
    Given a governed file "AGENT.adoc" containing
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de git push sans permission explicite.
      """
    And I run ingestGovernance on it
    When I wire the ingestion hook into a ToolRegistry
    Then ToolRegistry blocks "exec_shell" with command "git push origin main"

  @pre_hook
  Scenario: Ingested RULE INTERDIRE allows git push dry-run via exec_shell in ToolRegistry
    Given a governed file "AGENT.adoc" containing
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de git push sauf avec --dry-run.
      """
    And I run ingestGovernance on it
    When I wire the ingestion hook into a ToolRegistry
    Then ToolRegistry allows "exec_shell" with command "git push --dry-run origin main"

  @gradle_task
  Scenario: Ingested RULE INTERDIRE blocks gradle publish via exec_gradle in ToolRegistry
    Given a governed file "AGENT.adoc" containing
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de ./gradlew publish sans verification.
      """
    And I run ingestGovernance on it
    When I wire the ingestion hook into a ToolRegistry
    Then ToolRegistry blocks "exec_gradle" with task "publish"

  @multi_rule
  Scenario: Ingested multiple rules block matching commands in ToolRegistry
    Given a governed file "AGENT.adoc" containing
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de commit sans permission explicite.
      **INTERDICTION FORMELLE** de merge sans permission explicite.
      **INTERDICTION FORMELLE** de git push sans flag --dry-run.
      """
    And I run ingestGovernance on it
    When I wire the ingestion hook into a ToolRegistry
    Then ToolRegistry blocks "exec_shell" with command "git commit -m 'test'"
    And ToolRegistry blocks "exec_shell" with command "git merge feature-branch"
    And ToolRegistry blocks "exec_shell" with command "git push origin main"
    And ToolRegistry allows "exec_shell" with command "git push --dry-run origin main"

  @unrelated
  Scenario: Ingested rules do not block unrelated tools in ToolRegistry
    Given a governed file "AGENT.adoc" containing
      """
      = Agent

      == Regles Absolues

      **INTERDICTION FORMELLE** de git push sans permission explicite.
      """
    And I run ingestGovernance on it
    When I wire the ingestion hook into a ToolRegistry
    Then ToolRegistry allows "exec_gradle" with task "build"
    And ToolRegistry allows "exec_shell" with command "ls -la"
