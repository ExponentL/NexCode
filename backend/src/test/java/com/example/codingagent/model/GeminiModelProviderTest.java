package com.example.codingagent.model;

import com.example.codingagent.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class GeminiModelProviderTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;
    private AgentProperties properties;
    private GeminiModelProvider provider;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);

        properties = new AgentProperties();
        properties.getModel().setProvider("gemini");
        properties.getModel().setName("gemini-1.5-pro");
        properties.getModel().setApiKey("AIzaSyTestSecret12345");
        properties.getModel().setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        properties.getModel().setTimeoutSeconds(5);

        provider = new GeminiModelProvider(properties, mockHttpClient);
    }

    @Test
    void testIsConfigured() {
        assertTrue(provider.isConfigured());
        properties.getModel().setApiKey("");
        assertFalse(provider.isConfigured());
    }

    @Test
    void testSuccessfulGeneration() throws Exception {
        String responseJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"action\\": \\"SEARCH\\", \\"query\\": \\"AuthController\\"}"
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ModelRequest request = ModelRequest.builder()
                .messages(List.of(ChatMessage.user("Search for auth controller")))
                .build();

        ModelResponse resp = provider.generate(request);
        assertNotNull(resp);
        assertTrue(resp.content().contains("SEARCH"));
        assertTrue(resp.content().contains("AuthController"));
    }

    @Test
    void testSecretRedactedOnError() throws Exception {
        when(mockResponse.statusCode()).thenReturn(400);
        when(mockResponse.body()).thenReturn("API key not valid: key=AIzaSyTestSecret12345");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ModelRequest request = ModelRequest.builder()
                .messages(List.of(ChatMessage.user("Test secret")))
                .build();

        ModelException ex = assertThrows(ModelException.class, () -> provider.generate(request));
        assertFalse(ex.getMessage().contains("AIzaSyTestSecret12345"));
        assertTrue(ex.getMessage().contains("[REDACTED_API_KEY]") || ex.getMessage().contains("key=[REDACTED]"));
    }
}
