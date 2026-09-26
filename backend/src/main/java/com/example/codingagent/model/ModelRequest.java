package com.example.codingagent.model;

import java.util.Collections;
import java.util.List;

public record ModelRequest(
        List<ChatMessage> messages,
        List<ToolDefinition> tools,
        Double temperature,
        Integer maxTokens
) {
    public ModelRequest {
        if (messages == null) {
            messages = Collections.emptyList();
        }
        if (tools == null) {
            tools = Collections.emptyList();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ChatMessage> messages = Collections.emptyList();
        private List<ToolDefinition> tools = Collections.emptyList();
        private Double temperature = 0.2;
        private Integer maxTokens = 4096;

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder tools(List<ToolDefinition> tools) {
            this.tools = tools;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public ModelRequest build() {
            return new ModelRequest(messages, tools, temperature, maxTokens);
        }
    }
}
