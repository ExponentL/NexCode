package com.example.codingagent.model;

import java.util.regex.Pattern;

/**
 * Utility to sanitize logs and exception messages to prevent secret or API key leakage.
 */
public final class SecretSanitizer {

    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[a-zA-Z0-9_\\-\\.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern API_KEY_QUERY_PARAM = Pattern.compile("(?i)([?&]key=)[a-zA-Z0-9_\\-\\.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern X_API_KEY_PATTERN = Pattern.compile("(?i)(x-api-key\\s*[:=]\\s*)[a-zA-Z0-9_\\-\\.]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERIC_SECRET_PATTERN = Pattern.compile("(?i)(api[_-]?key|secret|token|password)[\"']?\\s*[:=]\\s*[\"']?([a-zA-Z0-9_\\-\\.]+)", Pattern.CASE_INSENSITIVE);

    private SecretSanitizer() {}

    /**
     * Replaces explicit API key and known sensitive patterns with masked placeholders.
     */
    public static String sanitize(String input, String activeApiKey) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;
        if (activeApiKey != null && !activeApiKey.isBlank() && activeApiKey.length() > 4) {
            result = result.replace(activeApiKey, "[REDACTED_API_KEY]");
        }

        String envAiKey = System.getenv("AI_API_KEY");
        if (envAiKey != null && !envAiKey.isBlank() && envAiKey.length() > 4) {
            result = result.replace(envAiKey, "[REDACTED_API_KEY]");
        }

        result = BEARER_PATTERN.matcher(result).replaceAll("Bearer [REDACTED]");
        result = API_KEY_QUERY_PARAM.matcher(result).replaceAll("$1[REDACTED]");
        result = X_API_KEY_PATTERN.matcher(result).replaceAll("$1[REDACTED]");
        result = GENERIC_SECRET_PATTERN.matcher(result).replaceAll("$1=[REDACTED]");

        return result;
    }

    public static String maskKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "NOT_SET";
        }
        if (apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
