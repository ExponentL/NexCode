package com.example.codingagent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretSanitizerTest {

    @Test
    void testMaskExplicitApiKey() {
        String secret = "sk-proj-supersecret123456789";
        String message = "Error calling endpoint with key: " + secret + " on server";
        String sanitized = SecretSanitizer.sanitize(message, secret);

        assertFalse(sanitized.contains(secret));
        assertTrue(sanitized.contains("[REDACTED_API_KEY]"));
    }

    @Test
    void testMaskBearerToken() {
        String message = "Authorization: Bearer sk-ant-api03-abcdefghijklmnop123456";
        String sanitized = SecretSanitizer.sanitize(message, null);

        assertFalse(sanitized.contains("abcdefghijklmnop"));
        assertTrue(sanitized.contains("Bearer [REDACTED]"));
    }

    @Test
    void testMaskQueryParamKey() {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=AIzaSyA_SampleSecret12345";
        String sanitized = SecretSanitizer.sanitize(url, null);

        assertFalse(sanitized.contains("AIzaSyA_SampleSecret12345"));
        assertTrue(sanitized.contains("key=[REDACTED]"));
    }

    @Test
    void testMaskHeaderKey() {
        String header = "x-api-key: secret-value-99999";
        String sanitized = SecretSanitizer.sanitize(header, null);

        assertFalse(sanitized.contains("secret-value-99999"));
        assertTrue(sanitized.contains("x-api-key: [REDACTED]"));
    }

    @Test
    void testMaskKeyUtility() {
        assertEquals("NOT_SET", SecretSanitizer.maskKey(null));
        assertEquals("NOT_SET", SecretSanitizer.maskKey("   "));
        assertEquals("***", SecretSanitizer.maskKey("12345"));
        assertEquals("sk-a...8901", SecretSanitizer.maskKey("sk-ant-test12345678901"));
    }
}
