package com.example.codingagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "agent.model.api-key=test-api-key",
        "agent.model.provider=openai"
})
class CodingAgentApplicationTests {
    @Test
    void contextLoads() {
    }
}
