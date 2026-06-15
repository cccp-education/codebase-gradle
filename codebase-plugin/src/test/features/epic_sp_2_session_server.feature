@epic_sp_2
Feature: Session Protocol Server — stdin/stdout JSON-lines daemon
  As an opencode thin client
  I want to run as a daemon processing multiple SessionPrompts via stdin
  So that session startup latency is eliminated for interactive use

  @server_single_prompt
  Scenario: Single prompt produces single response via stdin/stdout
    Given a SessionProtocolServer is configured with FakeLlmProvider
    When I feed the server with JSON-lines:
    """
    {"sessionId":"10000000-0000-0000-0000-000000000001","prompt":"Add dark mode toggle","maxActions":3}
    """
    Then the server responds with 1 JSON line
    And the response line contains status "COMPLETED" or "IN_PROGRESS"
    And the response line has sessionId "10000000-0000-0000-0000-000000000001"

  @server_multiple_prompts
  Scenario: Multiple prompts produce multiple responses
    Given a SessionProtocolServer is configured with FakeLlmProvider
    When I feed the server with JSON-lines:
    """
    {"sessionId":"20000000-0000-0000-0000-000000000002","prompt":"Fix typo","maxActions":2}
    {"sessionId":"30000000-0000-0000-0000-000000000003","prompt":"Add test","maxActions":2}
    """
    Then the server responds with 2 JSON lines
    And response line 1 has sessionId "20000000-0000-0000-0000-000000000002"
    And response line 2 has sessionId "30000000-0000-0000-0000-000000000003"

  @server_auto_session_id
  Scenario: Auto-generated sessionId when not provided
    Given a SessionProtocolServer is configured with FakeLlmProvider
    When I feed the server with JSON-lines:
    """
    {"prompt":"Auto-generate my session ID","maxActions":1}
    """
    Then the server responds with 1 JSON line
    And the response line has a valid UUID sessionId

  @server_error_resilience
  Scenario: Server continues after one prompt fails
    Given a SessionProtocolServer is configured with ThrowingLlmProvider only for prompt containing "fail"
    When I feed the server with JSON-lines:
    """
    {"sessionId":"40000000-0000-0000-0000-000000000004","prompt":"This should fail","maxActions":1}
    {"sessionId":"50000000-0000-0000-0000-000000000005","prompt":"This should succeed","maxActions":2}
    """
    Then the server responds with 2 JSON lines
    And response line 1 has status "ERROR"
    And response line 2 has status "COMPLETED" or "IN_PROGRESS"
