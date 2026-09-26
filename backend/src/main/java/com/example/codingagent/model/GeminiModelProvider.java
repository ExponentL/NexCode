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
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiModelProvider.class);

    private final AgentProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiModelProvider(AgentProperties properties) {
        this(properties, HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .proxy(java.net.ProxySelector.getDefault())
                .build());
    }

    public GeminiModelProvider(AgentProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String getProviderName() {
        return "gemini";
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
            throw new ModelException("Model provider 'gemini' is not configured. Please set AGENT_MODEL_API_KEY environment variable or in application.yml.");
        }

        String apiKey = properties.getModel().getApiKey().trim();
        int maxRetries = 3;
        long backoffMs = 1000;

        try {
            Map<String, Object> body = new HashMap<>();
            List<String> systemParts = new ArrayList<>();
            List<Map<String, Object>> contents = new ArrayList<>();

            for (ChatMessage msg : request.messages()) {
                if ("system".equalsIgnoreCase(msg.role())) {
                    if (msg.content() != null && !msg.content().isBlank()) {
                        systemParts.add(msg.content());
                    }
                } else {
                    String role = "model".equalsIgnoreCase(msg.role()) || "assistant".equalsIgnoreCase(msg.role()) ? "model" : "user";
                    if (!contents.isEmpty() && role.equals(contents.get(contents.size() - 1).get("role"))) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(contents.size() - 1).get("parts");
                        parts.add(Map.of("text", msg.content() != null ? msg.content() : ""));
                    } else {
                        List<Map<String, Object>> parts = new ArrayList<>();
                        parts.add(Map.of("text", msg.content() != null ? msg.content() : ""));
                        Map<String, Object> turn = new HashMap<>();
                        turn.put("role", role);
                        turn.put("parts", parts);
                        contents.add(turn);
                    }
                }
            }

            if (!systemParts.isEmpty()) {
                body.put("system_instruction", Map.of(
                        "parts", List.of(Map.of("text", String.join("\n\n", systemParts)))
                ));
            }

            if (contents.isEmpty()) {
                contents.add(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", "Please analyze the repository context and propose the next engineering action."))
                ));
            }
            body.put("contents", contents);

            Map<String, Object> genConfig = new HashMap<>();
            genConfig.put("temperature", request.temperature() != null ? request.temperature() : properties.getModel().getTemperature());
            if (request.maxTokens() != null) {
                genConfig.put("maxOutputTokens", request.maxTokens());
            }
            body.put("generationConfig", genConfig);

            String jsonPayload = objectMapper.writeValueAsString(body);

            String baseUrl = properties.getModel().getBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String url = String.format("%s/models/%s:generateContent?key=%s",
                    baseUrl,
                    properties.getModel().getName(),
                    apiKey);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getModel().getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    log.debug("Dispatching request to Gemini API (attempt {}/{})", attempt, maxRetries);
                    HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    int status = httpResponse.statusCode();

                    if (status == 429 || (status >= 500 && status <= 504)) {
                        String sanitized = SecretSanitizer.sanitize(httpResponse.body(), apiKey);
                        if (attempt < maxRetries) {
                            log.warn("Gemini API returned HTTP {} (rate-limit/server error). Retrying in {}ms... Error: {}",
                                    status, backoffMs, sanitized);
                            Thread.sleep(backoffMs);
                            backoffMs *= 2;
                            continue;
                        } else {
                            throw new ModelException("External Gemini API error [HTTP " + status + "]: " + sanitized);
                        }
                    }

                    if (status >= 400) {
                        String sanitized = SecretSanitizer.sanitize(httpResponse.body(), apiKey);
                        log.error("Gemini API error HTTP {}: {}", status, sanitized);
                        throw new ModelException("External Gemini API error [HTTP " + status + "]: " + sanitized);
                    }

                    JsonNode root = objectMapper.readTree(httpResponse.body());
                    StringBuilder textContent = new StringBuilder();

                    JsonNode candidates = root.get("candidates");
                    if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode contentNode = candidates.get(0).get("content");
                        if (contentNode != null && contentNode.hasNonNull("parts")) {
                            for (JsonNode part : contentNode.get("parts")) {
                                if (part.hasNonNull("text")) {
                                    textContent.append(part.get("text").asText());
                                }
                            }
                        }
                    }

                    return new ModelResponse(textContent.toString(), List.of(), "stop", Map.of());

                } catch (HttpTimeoutException te) {
                    if (attempt < maxRetries) {
                        log.warn("Gemini request timed out (attempt {}/{}). Retrying in {}ms...", attempt, maxRetries, backoffMs);
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    } else {
                        throw new ModelException("Gemini API timed out after " + properties.getModel().getTimeoutSeconds() + " seconds", te);
                    }
                } catch (IOException ioe) {
                    String detail = extractExceptionMessage(ioe);
                    if (attempt < maxRetries) {
                        log.warn("Gemini API network/connection error (attempt {}/{}): {}. Retrying in {}ms...", attempt, maxRetries, detail, backoffMs);
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    } else {
                        log.error("Failed to communicate with Gemini API after {} attempts: {}", maxRetries, detail, ioe);
                        throw new ModelException("Gemini API communication error: " + detail, ioe);
                    }
                }
            }
            throw new ModelException("Exceeded maximum retries communicating with Gemini API.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("Gemini API request interrupted: " + e.getMessage(), e);
        } catch (IOException e) {
            String detail = extractExceptionMessage(e);
            log.error("Gemini API error: {}", detail, e);
            throw new ModelException("Gemini API communication error: " + detail, e);
        }
    }

    private String extractExceptionMessage(Throwable t) {
        if (t == null) return "Unknown I/O error";
        if (t.getMessage() != null && !t.getMessage().isBlank()) {
            return t.getMessage();
        }
        if (t.getCause() != null && t.getCause().getMessage() != null && !t.getCause().getMessage().isBlank()) {
            return t.getClass().getSimpleName() + ": " + t.getCause().getMessage();
        }
        return t.getClass().getSimpleName();
    }
}
