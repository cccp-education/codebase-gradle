@epic_sp_5
Feature: Session Protocol SP-5 — ToolEventStream événements structurés temps réel
  As a thin client (opencode terminal)
  I want to receive structured events (thinking/tool_call/tool_result/progress/error) in real-time
  So that I can display live progress to the user during vibecoding execution

  @sp5_event_stream_create
  Scenario: Create session produces event stream with all event types
    Given an SP-5 SessionProtocolTask with event stream enabled
    When I SP-5 execute action "create" with prompt "Add dark mode toggle"
    Then the SP-5 event stream contains at least 1 THINKING event
    And the SP-5 event stream contains at least 1 PROGRESS event
    And the SP-5 event stream contains at least 1 TOOL_CALL event
    And the SP-5 event stream contains at least 1 TOOL_RESULT event
    And the SP-5 event stream contains no ERROR events

  @sp5_event_stream_error
  Scenario: Error during execution produces ERROR events in stream
    Given an SP-5 SessionProtocolTask with event stream enabled and a failing LLM provider
    When I SP-5 execute action "create" with prompt "This will fail"
    Then the SP-5 event stream contains at least 1 ERROR event

  @sp5_event_stream_server
  Scenario: Server mode produces event stream for multiple prompts
    Given an SP-5 SessionProtocolServer with event stream enabled
    When I SP-5 send 2 prompts through the server
    Then the SP-5 event stream contains events for both sessions
    And the SP-5 event stream contains at least 2 THINKING events
