package com.example.codingagent.model;

import com.example.codingagent.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class OpenAiCompatibleModelProviderTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;
    private AgentProperties properties;
    private OpenAiCompatibleModelProvider provider;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);

        properties = new AgentProperties();
        properties.getModel().setProvider("openai");
        properties.getModel().setName("gpt-4o");
        properties.getModel().setApiKey("test-openai-secret-key-12345");
        properties.getModel().setBaseUrl("https://api.openai.com/v1");
        properties.getModel().setTimeoutSeconds(5);

        provider = new OpenAiCompatibleModelProvider(properties, mockHttpClient);
    }

    @Test
    void testIsConfigured() {
        assertTrue(provider.isConfigured());

        properties.getModel().setApiKey("");
        assertFalse(provider.isConfigured());

        properties.getModel().setApiKey(null);
        assertFalse(provider.isConfigured());
    }

    @Test
    void testSuccessfulGenerationWithText() throws Exception {
        String responseJson = """
                {
                  "choices": [
                    {
                      "finish_reason": "stop",
                      "message": {
                        "role": "assistant",
                        "content": "{\\"action\\": \\"READ_FILE\\", \\"path\\": \\"pom.xml\\"}"
                      }
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 100,
                    "completion_tokens": 20,
                    "total_tokens": 120
                  }
                }
                """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ModelRequest request = ModelRequest.builder()
                .messages(List.of(ChatMessage.user("Read the POM file")))
                .build();

        ModelResponse resp = provider.generate(request);
        assertNotNull(resp);
        assertEquals("stop", resp.finishReason());
        assertTrue(resp.content().contains("READ_FILE"));
        assertEquals(120, resp.usage().get("total_tokens"));
    }

    @Test
    void testRateLimitRetriesAndSucceeds() throws Exception {
        HttpResponse<String> rateLimitResponse = mock(HttpResponse.class);
        when(rateLimitResponse.statusCode()).thenReturn(429);
        when(rateLimitResponse.body()).thenReturn("{\"error\": \"Rate limit exceeded\"}");

        HttpResponse<String> successResponse = mock(HttpResponse.class);
        when(successResponse.statusCode()).thenReturn(200);
        when(successResponse.body()).thenReturn("""
                {
                  "choices": [
                    {
                      "finish_reason": "stop",
                      "message": {
                        "role": "assistant",
                        "content": "{\\"action\\": \\"FINISH\\"}"
                      }
                    }
                  ]
                }
                """);

        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(rateLimitResponse)
                .thenReturn(successResponse);

        ModelRequest request = ModelRequest.builder()
                .messages(List.of(ChatMessage.user("Proceed")))
                .build();

        ModelResponse resp = provider.generate(request);
        assertNotNull(resp);
        assertTrue(resp.content().contains("FINISH"));
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void testApiKeyRedactedOnServerError() throws Exception {
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn("Unauthorized access with key test-openai-secret-key-12345");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ModelRequest request = ModelRequest.builder()
                .messages(List.of(ChatMessage.user("Test secret leakage")))
                .build();

        ModelException ex = assertThrows(ModelException.class, () -> provider.generate(request));
        assertFalse(ex.getMessage().contains("test-openai-secret-key-12345"), "API key must be redacted in exception");
        assertTrue(ex.getMessage().contains("[REDACTED_API_KEY]"));
    }
}
