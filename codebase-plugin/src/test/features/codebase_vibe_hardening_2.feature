@vibe-hardening-2
Feature: Vibe Hardening 2 — write_file size guard, expectedOutput comparison, gradle flag+value, remaining tasks coerce
  As a codebase-gradle maintainer
  I want to harden the vibecoding execution layer against size overflow, semantic verification gaps, flag parsing, and misleading negative counts
  So that the autonomous agent produces trustworthy progress reports and accepts valid flag arguments

  Background:
    Given a vibe hardening 2 world is initialized

  Scenario: write_file rejects content exceeding MAX_WRITE_CHARS
    When the vibe hardening 2 write_file tool is called with content of 1000001 chars
    Then the vibe hardening 2 write_file tool rejects the content with a size error

  Scenario: TaskResultVerifier matches custom expectedOutput against stdout
    When the vibe hardening 2 verifier checks stdout "BUILD SUCCESSFUL\n3 tests passed" against expected "3 tests passed"
    Then the vibe hardening 2 verifier returns SUCCESS

  Scenario: ExecGradleTool accepts a flag with a separate value argument
    When the vibe hardening 2 gradle tool validates task "test --tests FastTest"
    Then the vibe hardening 2 gradle tool accepts the task

  Scenario: Plan remaining tasks is coerced to zero when executed exceeds plan total
    When the vibe hardening 2 prompt is built for a 1-task plan with 3 executed tasks
    Then the vibe hardening 2 prompt remaining tasks line is "Plan remaining tasks: 0"