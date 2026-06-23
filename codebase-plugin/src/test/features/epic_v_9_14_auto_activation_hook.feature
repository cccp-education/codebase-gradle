@epic_v_9_14
Feature: Auto-activation of governance enforcement hook
  As a codebase-gradle developer
  I want VibecodingTask and SessionProtocolTask to activate the enforcement hook automatically
  So that exec_shell git push and exec_gradle publish are blocked without manual wiring

  @vibecoding_task
  Scenario: VibecodingTask dry-run activates hook and blocks git push
    Given a governed project with file "AGENT.adoc" containing
      """
      = Agent

      * NE DOIT JAMAIS git push sans permission explicite.
      """
    When I run VibecodingTask dryRun with intention "V-9.14 auto-activation test"
    Then the task toolRegistry blocks "exec_shell" with command "git push origin main"

  @session_protocol_task
  Scenario: SessionProtocolTask activates hook and blocks gradle publish
    Given a governed project with file "AGENT.adoc" containing
      """
      = Agent

      * NE DOIT JAMAIS ./gradlew publish sans verification.
      """
    When I run SessionProtocolTask with prompt "V-9.14 publish blocking test"
    Then the task toolRegistry blocks "exec_gradle" with task "publish"
