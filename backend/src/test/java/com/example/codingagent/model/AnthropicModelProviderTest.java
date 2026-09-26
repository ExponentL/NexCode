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
class AnthropicModelProviderTest {

    private HttpClient mockHttpClient;
    private HttpResponse<String> mockResponse;
    private AgentProperties properties;
    private AnthropicModelProvider provider;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockResponse = mock(HttpResponse.class);

        properties = new AgentProperties();
        properties.getModel().setProvider("anthropic");
        properties.getModel().setName("claude-3-5-sonnet-20241022");
        properties.getModel().setApiKey("sk-ant-api03-secret12345");
        properties.getModel().setBaseUrl("https://api.anthropic.com/v1");
        properties.getModel().setTimeoutSeconds(5);

        provider = new AnthropicModelProvider(properties, mockHttpClient);
    }

    @Test
    void testIsConfigured() {
        assertTrue(provider.isConfigured());
        properties.getModel().setApiKey("");
        assertFalse(provider.isConfigured());
    }

    @Test
    void testSuccessfulGenerationWithToolUse() throws Exception {
        String responseJson = """
                {
                  "id": "msg_01",
                  "type": "message",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "text",
                      "text": "Locating files"
                    },
                    {
                      "type": "tool_use",
                      "id": "toolu_01",
                      "name": "file_tool",
                      "input": {
                        "action": "READ_FILE",
                        "path": "src/App.java"
                      }
                    }
                  ],
                  "stop_reason": "tool_use"
                }
                """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ModelRequest request = ModelRequest.builder()
                .messages(List.of(ChatMessage.user("Find App.java")))
                .build();

        ModelResponse resp = provider.generate(request);
        assertNotNull(resp);
        assertEquals("Locating files", resp.content());
        assertTrue(resp.hasToolCalls());
        assertEquals(1, resp.toolCalls().size());
        assertEquals("file_tool", resp.toolCalls().get(0).name());
        assertTrue(resp.toolCalls().get(0).argumentsJson().contains("src/App.java"));
    }

    @Test
    void testSecretRedactedOnError() throws Exception {
        when(mockResponse.statusCode()).thenReturn(401);
        when(mockResponse.body()).thenReturn("Invalid API key: sk-ant-api03-secret12345");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ModelRequest request = ModelRequest.builder()
                .messages(List.of(ChatMessage.user("Test")))
                .build();

        ModelException ex = assertThrows(ModelException.class, () -> provider.generate(request));
        assertFalse(ex.getMessage().contains("sk-ant-api03-secret12345"));
        assertTrue(ex.getMessage().contains("[REDACTED_API_KEY]"));
    }
}
