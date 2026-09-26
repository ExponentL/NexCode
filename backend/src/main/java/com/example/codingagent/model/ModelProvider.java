package com.example.codingagent.model;

/**
 * Pluggable abstraction layer for communicating with external foundation models.
 * Allows decoupling the agent controller and harness from specific providers.
 */
public interface ModelProvider {

    /**
     * @return Unique provider identifier (e.g., "openai", "anthropic", "gemini")
     */
    String getProviderName();

    /**
     * @return Currently configured foundation model name (e.g., "gpt-4o", "claude-3-5-sonnet-20241022", "gemini-1.5-pro")
     */
    String getModelName();

    /**
     * Synchronously generates an AI completion or tool calls from the foundation model.
     *
     * @param request Input messages, tools, and execution parameters
     * @return Generated model response
     * @throws ModelException if communication or credentials fail
     */
    ModelResponse generate(ModelRequest request) throws ModelException;

    /**
     * Verifies whether the provider has the necessary credentials and connectivity configured.
     *
     * @return true if configured with required API key/endpoint
     */
    boolean isConfigured();
}
