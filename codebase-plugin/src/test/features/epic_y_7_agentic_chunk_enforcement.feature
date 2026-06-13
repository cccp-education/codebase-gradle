@epic_y_7
Feature: Agentic Chunk Enforcement — Rules Become Executable Constraints
  As a codebase-gradle developer
  I want rules written in AGENT.adoc to become constraints enforced by the ToolRegistry
  So that agentic literature is not just advisory but executable

  @enforce_git_push
  Scenario: A rule forbidding git push without --dry-run blocks the forbidden action
    Given the agentic schema is initialized for enforcement
    And an AGENT.adoc file with a rule "INTERDIRE git push sans flag --dry-run"
    When I ingest the files and register enforcement rules
    Then the enforcement blocks "exec_shell" with command "git push origin main"
    And the enforcement allows "exec_shell" with command "git push --dry-run origin main"

  @enforce_commit
  Scenario: A rule forbidding commit blocks the forbidden action
    Given the agentic schema is initialized for enforcement
    And an AGENT.adoc file with a rule "INTERDICTION FORMELLE de commit sans permission explicite"
    When I ingest the files and register enforcement rules
    Then the enforcement blocks "exec_shell" with command "git commit -m 'test'"

  @enforce_merge
  Scenario: A rule forbidding merge blocks the forbidden action
    Given the agentic schema is initialized for enforcement
    And an AGENT.adoc file with a rule "INTERDICTION FORMELLE de merge sans permission explicite"
    When I ingest the files and register enforcement rules
    Then the enforcement blocks "exec_shell" with command "git merge feature-branch"

  @enforce_publish
  Scenario: A rule forbidding gradle publish blocks the forbidden action
    Given the agentic schema is initialized for enforcement
    And an AGENT.adoc file with a rule "INTERDICTION FORMELLE de ./gradlew publish sans verification"
    When I ingest the files and register enforcement rules
    Then the enforcement blocks "exec_gradle" with task "publish"

  @enforce_publish_dry_run
  Scenario: A rule forbidding gradle publish without --dry-run allows the safe variant
    Given the agentic schema is initialized for enforcement
    And an AGENT.adoc file with a rule "INTERDICTION FORMELLE de ./gradlew publish sans flag --dry-run"
    When I ingest the files and register enforcement rules
    Then the enforcement blocks "exec_gradle" with task "publish"
    And the enforcement allows "exec_gradle" with task "publish --dry-run"

  @enforce_multiple_rules
  Scenario: Multiple rules from the same AGENT.adoc are all enforced
    Given the agentic schema is initialized for enforcement
    And an AGENT.adoc file with multiple rules forbidding commit, merge, and push
    When I ingest the files and register enforcement rules
    Then the enforcement blocks "exec_shell" with command "git commit -m 'test'"
    And the enforcement blocks "exec_shell" with command "git merge feature-branch"
    And the enforcement blocks "exec_shell" with command "git push origin main"
    And the enforcement allows "exec_shell" with command "git push --dry-run origin main"

  @enforce_unrelated
  Scenario: Unrelated tool calls are not blocked by enforcement rules
    Given the agentic schema is initialized for enforcement
    And an AGENT.adoc file with a rule "INTERDIRE git push sans flag --dry-run"
    When I ingest the files and register enforcement rules
    Then the enforcement allows "exec_gradle" with task "build"
    And the enforcement allows "exec_shell" with command "ls -la"
