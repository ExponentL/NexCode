package com.example.codingagent.model;

import com.example.codingagent.config.AgentProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnthropicModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(AnthropicModelProvider.class);

    private final AgentProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public AnthropicModelProvider(AgentProperties properties) {
        this(properties, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .proxy(java.net.ProxySelector.getDefault())
                .build());
    }

    public AnthropicModelProvider(AgentProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String getProviderName() {
        return "anthropic";
    }

    @Override
    public String getModelName() {
        return properties.getModel().getName();
    }

    @Override
    public boolean isConfigured() {
        String apiKey = properties.getModel().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public ModelResponse generate(ModelRequest request) throws ModelException {
        if (!isConfigured()) {
            throw new ModelException("Model provider 'anthropic' is not configured. Please set AGENT_MODEL_API_KEY environment variable or in application.yml.");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", properties.getModel().getName());
            body.put("max_tokens", request.maxTokens() != null ? request.maxTokens() : 4096);
            if (request.temperature() != null) {
                body.put("temperature", request.temperature());
            }

            StringBuilder systemPrompt = new StringBuilder();
            List<Map<String, Object>> messagesList = new ArrayList<>();
            for (ChatMessage msg : request.messages()) {
                if ("system".equalsIgnoreCase(msg.role())) {
                    if (!systemPrompt.isEmpty()) systemPrompt.append("\n\n");
                    systemPrompt.append(msg.content());
                } else {
                    Map<String, Object> m = new HashMap<>();
                    m.put("role", msg.role());
                    m.put("content", msg.content() != null ? msg.content() : "");
                    messagesList.add(m);
                }
            }

            if (!systemPrompt.isEmpty()) {
                body.put("system", systemPrompt.toString());
            }
            body.put("messages", messagesList);

            if (request.tools() != null && !request.tools().isEmpty()) {
                List<Map<String, Object>> toolsList = new ArrayList<>();
                for (ToolDefinition tool : request.tools()) {
                    toolsList.add(Map.of(
                            "name", tool.name(),
                            "description", tool.description(),
                            "input_schema", tool.parameters()
                    ));
                }
                body.put("tools", toolsList);
            }

            String apiKey = properties.getModel().getApiKey().trim();
            int maxRetries = 3;
            long backoffMs = 1000;

            String jsonPayload = objectMapper.writeValueAsString(body);
            String baseUrl = properties.getModel().getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            URI uri = URI.create(baseUrl + "/messages");

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(properties.getModel().getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    int status = httpResponse.statusCode();

                    if (status == 429 || status == 529 || (status >= 500 && status <= 504)) {
                        String sanitized = SecretSanitizer.sanitize(httpResponse.body(), apiKey);
                        if (attempt < maxRetries) {
                            log.warn("Anthropic API returned HTTP {} (rate-limit/overloaded). Retrying in {}ms... Error: {}",
                                    status, backoffMs, sanitized);
                            Thread.sleep(backoffMs);
                            backoffMs *= 2;
                            continue;
                        } else {
                            throw new ModelException("External Anthropic API error [HTTP " + status + "]: " + sanitized);
                        }
                    }

                    if (status >= 400) {
                        String sanitized = SecretSanitizer.sanitize(httpResponse.body(), apiKey);
                        log.error("Anthropic API error HTTP {}: {}", status, sanitized);
                        throw new ModelException("External Anthropic API error [HTTP " + status + "]: " + sanitized);
                    }

                    JsonNode root = objectMapper.readTree(httpResponse.body());
                    StringBuilder textContent = new StringBuilder();
                    List<ToolCall> toolCalls = new ArrayList<>();

                    if (root.hasNonNull("content") && root.get("content").isArray()) {
                        for (JsonNode block : root.get("content")) {
                            String blockType = block.hasNonNull("type") ? block.get("type").asText() : "";
                            if ("text".equals(blockType)) {
                                textContent.append(block.get("text").asText());
                            } else if ("tool_use".equals(blockType)) {
                                String id = block.get("id").asText();
                                String name = block.get("name").asText();
                                String inputJson = block.has("input") ? block.get("input").toString() : "{}";
                                toolCalls.add(new ToolCall(id, name, inputJson));
                            }
                        }
                    }

                    String stopReason = root.hasNonNull("stop_reason") ? root.get("stop_reason").asText() : "stop";
                    return new ModelResponse(textContent.toString(), toolCalls, stopReason, Map.of());

                } catch (java.net.http.HttpTimeoutException te) {
                    if (attempt < maxRetries) {
                        log.warn("Anthropic request timed out (attempt {}/{}). Retrying in {}ms...", attempt, maxRetries, backoffMs);
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    } else {
                        throw new ModelException("Anthropic API timed out after " + properties.getModel().getTimeoutSeconds() + " seconds", te);
                    }
                }
            }
            throw new ModelException("Exceeded maximum retries communicating with Anthropic API.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("Anthropic API request interrupted: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ModelException("Anthropic API communication error: " + e.getMessage(), e);
        }
    }
}
