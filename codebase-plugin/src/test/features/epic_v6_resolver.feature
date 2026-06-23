@epic_v6_resolver
Feature: Ollama Cloud Resolver — deterministic factory mode
  As a codebase-gradle developer
  I want the resolver to build the Ollama cloud provider from the deterministic factory
  So that the pool is available without probing the network

  Background:
    Given no Ollama scan environment variables are set

  @factory
  Scenario: Resolver builds Ollama cloud provider from deterministic factory
    When I resolve provider for model "ollama"
    Then the provider is an OllamaLlmProvider
    And the provider pool contains 29 instances
    And the provider pool models cycle through the 2 authorized cloud models

  @explicit
  Scenario: Resolver honors OLLAMA_POOL_PORTS when set
    Given OLLAMA_POOL_PORTS is set to "11450"
    When I resolve provider for model "ollama"
    Then the provider is an OllamaLlmProvider
    And the provider pool contains 1 instance on port 11450

  @scanner
  Scenario: Resolver falls back to scanner when OLLAMA_SCAN_PORTS is true
    Given OLLAMA_SCAN_PORTS is set to "true"
    And the fake scanner reports live ports "11437,11438"
    When I resolve provider for model "ollama"
    Then the provider is an OllamaLlmProvider
    And the provider pool contains 2 instances
