package com.example.codingagent.model.action;

import com.example.codingagent.model.ToolCall;
import com.example.codingagent.tools.SecurityGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModelActionTest {

    private SecurityGuard securityGuard;
    private ModelActionParser parser;

    @BeforeEach
    void setUp() {
        securityGuard = new SecurityGuard();
        parser = new ModelActionParser();
    }

    @Test
    void testActionValidationRejectsPathTraversal(@TempDir Path tempDir) {
        ModelAction action = ModelAction.readFile("../../etc/shadow", "Attempt to read host secret");
        ActionValidationResult result = action.validate(tempDir.toString(), securityGuard);

        assertFalse(result.valid());
        assertTrue(result.errorMessage().contains("Path traversal"));
    }

    @Test
    void testActionValidationAcceptsSafePath(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("service.java"), "class Service {}");

        ModelAction action = ModelAction.readFile("service.java", "Read implementation");
        ActionValidationResult result = action.validate(tempDir.toString(), securityGuard);

        assertTrue(result.valid());
        assertNotNull(result.resolvedTarget());
    }

    @Test
    void testActionValidationRejectsDangerousCommand(@TempDir Path tempDir) {
        ModelAction action = ModelAction.runCommand("rm -rf /", "Destroy system");
        ActionValidationResult result = action.validate(tempDir.toString(), securityGuard);

        assertFalse(result.valid());
        assertTrue(result.errorMessage().contains("Dangerous command rejected"));
    }

    @Test
    void testActionValidationRejectsEmptySearchQuery(@TempDir Path tempDir) {
        ModelAction action = ModelAction.search("", "Search without query");
        ActionValidationResult result = action.validate(tempDir.toString(), securityGuard);

        assertFalse(result.valid());
        assertTrue(result.errorMessage().contains("non-empty 'query'"));
    }

    @Test
    void testParseFromText() {
        String jsonOutput = """
                ```json
                {
                  "action": "READ_FILE",
                  "path": "src/main/Auth.java",
                  "reason": "Examine token generation"
                }
                ```
                """;

        ModelAction action = parser.parseFromText(jsonOutput);

        assertNotNull(action);
        assertEquals(ActionType.READ_FILE, action.getAction());
        assertEquals("src/main/Auth.java", action.getPath());
        assertEquals("Examine token generation", action.getReason());
    }

    @Test
    void testParseFromToolCall() {
        ToolCall toolCall = new ToolCall("call_1", "file_tool", "{\"action\":\"WRITE_FILE\",\"path\":\"src/App.java\",\"content\":\"class App {}\"}");
        ModelAction action = parser.parseFromToolCall(toolCall);

        assertNotNull(action);
        assertEquals(ActionType.WRITE_FILE, action.getAction());
        assertEquals("src/App.java", action.getPath());
        assertEquals("class App {}", action.getContent());
    }
}
