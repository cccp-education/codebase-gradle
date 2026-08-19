@finetuning
Feature: FT-PIPELINE — Fine-tuning N1 pipeline (dataset → GGUF → manifest, iterative cycle, degraded fallback, validation threshold)
  As a codebase-gradle maintainer
  I want the fine-tuning pipeline to assemble a model, iterate until convergence, and degrade gracefully when Ollama is unavailable
  So that the autonomous expert fabrication loop is trustworthy and never crashes the caller

  Background:
    Given a finetuning world is initialized

  Scenario: Pipeline complet — dataset préparé, modèle créé, GGUF produit, expert enregistré
    Given a finetuning pipeline wired to an Ollama registry returning success
    When the finetuning pipeline fine-tunes a request with base model "gpt-oss:120b-cloud", dataset "docs/afnor/**/*.adoc" and output model "expert-cda"
    Then the finetuning pipeline returns a success result
    And the finetuning pipeline success result references output model "expert-cda"
    And the finetuning pipeline success result references a non-blank GGUF path
    And the finetuning pipeline success result reports 1 iteration and a validation score of 1.0

  Scenario: Cycle itératif converge après ajustement du corpus ratio
    Given a finetuning graph with a fake LLM proposing ratio 0.15, validating 0.5 then 0.8, and a fake pipeline always succeeding
    When the finetuning graph executes from an initial state with threshold 0.7 and max 3 iterations
    Then the finetuning graph final stage is CONVERGED
    And the finetuning graph final validation score is 0.8
    And the finetuning graph final iteration is 1

  Scenario: Fallback degraded quand Ollama registry est indisponible
    Given a finetuning pipeline wired to an Ollama registry returning a 503 failure
    When the finetuning pipeline fine-tunes a request with base model "gemma4:31b-cloud", dataset "docs/reac/**/*.adoc" and output model "expert-fpa"
    Then the finetuning pipeline returns a failure result
    And the finetuning pipeline failure reason mentions the registry failure
    And the finetuning pipeline failure preserves the original dataset "docs/reac/**/*.adoc"

  Scenario: Validation seuil atteint dès la première itération
    Given a finetuning graph with a fake LLM proposing ratio 0.10, validating 0.9, and a fake pipeline always succeeding
    When the finetuning graph executes from an initial state with threshold 0.7 and max 3 iterations
    Then the finetuning graph final stage is CONVERGED
    And the finetuning graph final validation score is 0.9
    And the finetuning graph final iteration is 0