package com.example.codingagent.model;

import com.example.codingagent.config.AgentProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelProviderFactoryTest {

    @Test
    void testSelectsOpenAiProvider() {
        AgentProperties props = new AgentProperties();
        props.getModel().setProvider("openai");

        ModelProvider mockOpenAi = new TestModelProvider("openai", "gpt-4o");
        ModelProvider mockAnthropic = new TestModelProvider("anthropic", "claude-3-5-sonnet");

        ModelProviderFactory factory = new ModelProviderFactory();
        ModelProvider primary = factory.primaryModelProvider(List.of(mockOpenAi, mockAnthropic), props);

        assertEquals("openai", primary.getProviderName());
        assertEquals("gpt-4o", primary.getModelName());
    }

    @Test
    void testSelectsAnthropicProviderCaseInsensitive() {
        AgentProperties props = new AgentProperties();
        props.getModel().setProvider("ANTHROPIC");

        ModelProvider mockOpenAi = new TestModelProvider("openai", "gpt-4o");
        ModelProvider mockAnthropic = new TestModelProvider("anthropic", "claude-3-5-sonnet");

        ModelProviderFactory factory = new ModelProviderFactory();
        ModelProvider primary = factory.primaryModelProvider(List.of(mockOpenAi, mockAnthropic), props);

        assertEquals("anthropic", primary.getProviderName());
    }

    @Test
    void testThrowsOnUnknownProvider() {
        AgentProperties props = new AgentProperties();
        props.getModel().setProvider("unknown-ai");

        ModelProvider mockOpenAi = new TestModelProvider("openai", "gpt-4o");

        ModelProviderFactory factory = new ModelProviderFactory();
        assertThrows(IllegalStateException.class, () ->
                factory.primaryModelProvider(List.of(mockOpenAi), props));
    }

    private static class TestModelProvider implements ModelProvider {
        private final String name;
        private final String model;

        TestModelProvider(String name, String model) {
            this.name = name;
            this.model = model;
        }

        @Override public String getProviderName() { return name; }
        @Override public String getModelName() { return model; }
        @Override public ModelResponse generate(ModelRequest request) { return ModelResponse.text("ok"); }
        @Override public boolean isConfigured() { return true; }
    }
}
