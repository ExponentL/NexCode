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
public class OpenAiCompatibleModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleModelProvider.class);

    private final AgentProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public OpenAiCompatibleModelProvider(AgentProperties properties) {
        this(properties, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .proxy(java.net.ProxySelector.getDefault())
                .build());
    }

    public OpenAiCompatibleModelProvider(AgentProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String getProviderName() {
        return "openai";
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
            throw new ModelException("Model provider 'openai' is not configured. Please set AGENT_MODEL_API_KEY environment variable or in application.yml.");
        }

        String apiKey = properties.getModel().getApiKey().trim();
        int maxRetries = 3;
        long backoffMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> requestBody = buildRequestBody(request);
                String jsonPayload = objectMapper.writeValueAsString(requestBody);

                String baseUrl = properties.getModel().getBaseUrl();
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                URI uri = URI.create(baseUrl + "/chat/completions");

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(uri)
                        .timeout(Duration.ofSeconds(properties.getModel().getTimeoutSeconds()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("User-Agent", "Nexcode-Agent/1.0")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                log.debug("Dispatching request to model API: {} (attempt {}/{})", uri, attempt, maxRetries);
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                int status = httpResponse.statusCode();
                if (status == 429 || (status >= 500 && status <= 504)) {
                    String sanitized = SecretSanitizer.sanitize(httpResponse.body(), apiKey);
                    boolean isHardQuotaExceeded = status == 429 && (sanitized.contains("free-model token quota")
                            || sanitized.contains("insufficient_quota")
                            || sanitized.contains("quota_exceeded"));

                    if (isHardQuotaExceeded) {
                        log.error("Model API quota reached: {}", sanitized);
                        throw new ModelException("External Model API error [HTTP 429 - Quota Exceeded]: " + sanitized);
                    }

                    if (attempt < maxRetries) {
                        log.warn("Model API returned HTTP {} (rate-limit/server error). Retrying in {}ms... Error: {}",
                                status, backoffMs, sanitized);
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                        continue;
                    } else {
                        log.error("Model API returned HTTP {} after {} attempts: {}", status, maxRetries, sanitized);
                        throw new ModelException("External Model API error [HTTP " + status + "]: " + sanitized);
                    }
                }

                if (status >= 400) {
                    String sanitized = SecretSanitizer.sanitize(httpResponse.body(), apiKey);
                    log.error("Foundation model API returned HTTP {}: {}", status, sanitized);
                    throw new ModelException("External Model API error [HTTP " + status + "]: " + sanitized);
                }

                return parseResponse(httpResponse.body());

            } catch (java.net.http.HttpTimeoutException te) {
                if (attempt < maxRetries) {
                    log.warn("Model request timed out after {}s (attempt {}/{}). Retrying in {}ms...",
                            properties.getModel().getTimeoutSeconds(), attempt, maxRetries, backoffMs);
                    try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    backoffMs *= 2;
                } else {
                    throw new ModelException("Foundation model API request timed out after " + properties.getModel().getTimeoutSeconds() + " seconds.", te);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ModelException("Model API request was interrupted: " + e.getMessage(), e);
            } catch (IOException e) {
                String errorDetail = (e.getMessage() != null && !e.getMessage().isBlank())
                        ? e.getMessage()
                        : (e.getCause() != null && e.getCause().getMessage() != null
                                ? e.getCause().getMessage()
                                : e.getClass().getSimpleName() + " (Network socket connection refused or reset)");
                if (attempt < maxRetries) {
                    log.warn("Network error communicating with model API (attempt {}/{}): {}", attempt, maxRetries, errorDetail);
                    try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    backoffMs *= 2;
                } else {
                    throw new ModelException("Failed to communicate with foundation model API: " + SecretSanitizer.sanitize(errorDetail, apiKey), e);
                }
            }
        }
        throw new ModelException("Exceeded maximum retries communicating with foundation model API.");
    }

    private Map<String, Object> buildRequestBody(ModelRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.getModel().getName());
        body.put("temperature", request.temperature() != null ? request.temperature() : properties.getModel().getTemperature());
        if (request.maxTokens() != null) {
            body.put("max_tokens", request.maxTokens());
        }

        List<Map<String, Object>> messagesList = new ArrayList<>();
        for (ChatMessage msg : request.messages()) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", msg.role());
            if (msg.content() != null) {
                m.put("content", msg.content());
            }
            if (msg.toolCallId() != null) {
                m.put("tool_call_id", msg.toolCallId());
            }
            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                List<Map<String, Object>> calls = new ArrayList<>();
                for (ToolCall tc : msg.toolCalls()) {
                    calls.add(Map.of(
                            "id", tc.id(),
                            "type", "function",
                            "function", Map.of(
                                    "name", tc.name(),
                                    "arguments", tc.argumentsJson()
                            )
                    ));
                }
                m.put("tool_calls", calls);
            }
            messagesList.add(m);
        }
        body.put("messages", messagesList);

        if (request.tools() != null && !request.tools().isEmpty()) {
            List<Map<String, Object>> toolsList = new ArrayList<>();
            for (ToolDefinition tool : request.tools()) {
                toolsList.add(Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.name(),
                                "description", tool.description(),
                                "parameters", tool.parameters()
                        )
                ));
            }
            body.put("tools", toolsList);
        }

        return body;
    }

    private ModelResponse parseResponse(String responseJson) throws IOException {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new ModelException("Invalid response format from foundation model: missing choices");
        }

        JsonNode firstChoice = choices.get(0);
        JsonNode messageNode = firstChoice.get("message");
        String content = messageNode.hasNonNull("content") ? messageNode.get("content").asText() : "";
        String finishReason = firstChoice.hasNonNull("finish_reason") ? firstChoice.get("finish_reason").asText() : "stop";

        List<ToolCall> toolCalls = new ArrayList<>();
        if (messageNode.hasNonNull("tool_calls") && messageNode.get("tool_calls").isArray()) {
            for (JsonNode tcNode : messageNode.get("tool_calls")) {
                String id = tcNode.hasNonNull("id") ? tcNode.get("id").asText() : java.util.UUID.randomUUID().toString();
                JsonNode fnNode = tcNode.get("function");
                if (fnNode != null) {
                    String fnName = fnNode.hasNonNull("name") ? fnNode.get("name").asText() : "";
                    String args = fnNode.hasNonNull("arguments") ? fnNode.get("arguments").asText() : "{}";
                    toolCalls.add(new ToolCall(id, fnName, args));
                }
            }
        }

        Map<String, Object> usage = new HashMap<>();
        if (root.hasNonNull("usage")) {
            JsonNode uNode = root.get("usage");
            if (uNode.has("prompt_tokens")) usage.put("prompt_tokens", uNode.get("prompt_tokens").asInt());
            if (uNode.has("completion_tokens")) usage.put("completion_tokens", uNode.get("completion_tokens").asInt());
            if (uNode.has("total_tokens")) usage.put("total_tokens", uNode.get("total_tokens").asInt());
        }

        return new ModelResponse(content, toolCalls, finishReason, usage);
    }
}
