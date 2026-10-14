@koog-pipeline
Feature: KOOG-PIPELINE-DEDUP — generic koog pipeline skeleton (N1)
  As a codebase-gradle maintainer
  I want one shared propose -> validate -> generate pipeline skeleton
  So that capsule and slider stop duplicating the koog orchestration

  Background:
    Given a koog pipeline world is initialized

  Scenario: Happy path runs propose, validate and generate in order
    Given a koog pipeline that proposes a valid plan
    When the koog pipeline executes
    Then the koog pipeline call order is "propose,validate,generate"
    And the koog pipeline final stage is "CONTENT_GENERATED"
    And the koog pipeline output is "generated"

  Scenario: An invalid context short-circuits before generate
    Given a koog pipeline that proposes a valid plan
    And a koog pipeline that rejects validation with "bad plan"
    When the koog pipeline executes
    Then the koog pipeline call order is "propose,validate"
    And the koog pipeline final stage is "FAILED"
    And the koog pipeline validation error is "bad plan"

  Scenario: A throwing propose node is mapped to a failed state
    Given a koog pipeline that throws on propose with "boom"
    When the koog pipeline executes
    Then the koog pipeline call order is "propose"
    And the koog pipeline final stage is "FAILED"
    And the koog pipeline error is "ProposeFailed: boom"

  Scenario: The shared skeleton exposes a non-blank mermaid topology
    Given a koog pipeline that proposes a valid plan
    When the koog pipeline mermaid diagram is requested
    Then the koog pipeline mermaid diagram is not blank
