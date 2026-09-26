package com.example.codingagent.model.action;

import com.example.codingagent.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelActionParserTest {

    private ModelActionParser parser;

    @BeforeEach
    void setUp() {
        parser = new ModelActionParser();
    }

    @Test
    void testParseDirectJsonObject() {
        String json = """
                {
                  "action": "READ_FILE",
                  "path": "src/main/java/App.java",
                  "reason": "Examine entrypoint",
                  "startLine": 1,
                  "endLine": 50
                }
                """;

        ModelAction action = parser.parseFromText(json);
        assertNotNull(action);
        assertEquals(ActionType.READ_FILE, action.getAction());
        assertEquals("src/main/java/App.java", action.getPath());
        assertEquals("Examine entrypoint", action.getReason());
        assertEquals(1, action.getStartLine());
        assertEquals(50, action.getEndLine());
    }

    @Test
    void testParseMarkdownFencedJson() {
        String markdown = """
                I will now search for login authentication classes.
                ```json
                {
                  "action": "SEARCH",
                  "query": "authentication",
                  "reason": "Locate security configuration"
                }
                ```
                Please proceed.
                """;

        ModelAction action = parser.parseFromText(markdown);
        assertNotNull(action);
        assertEquals(ActionType.SEARCH, action.getAction());
        assertEquals("authentication", action.getQuery());
        assertEquals("Locate security configuration", action.getReason());
    }

    @Test
    void testParseEmbeddedJsonWithoutFences() {
        String prose = """
                Based on my review, here is the action:
                {"action": "WRITE_FILE", "path": "src/Utils.java", "content": "public class Utils {}", "reason": "Add utility methods"}
                Hope this helps!
                """;

        ModelAction action = parser.parseFromText(prose);
        assertNotNull(action);
        assertEquals(ActionType.WRITE_FILE, action.getAction());
        assertEquals("src/Utils.java", action.getPath());
        assertEquals("public class Utils {}", action.getContent());
    }

    @Test
    void testParseNullOrInvalidTextReturnsNull() {
        assertNull(parser.parseFromText(null));
        assertNull(parser.parseFromText(""));
        assertNull(parser.parseFromText("   "));
        assertNull(parser.parseFromText("This is plain conversation without any action or json."));
        assertNull(parser.parseFromText("{ malformed json : missing quotes }"));
    }

    @Test
    void testParseFromToolCallFileTool() {
        ToolCall tc = new ToolCall("call_1", "file_tool", """
                {"action": "WRITE_FILE", "path": "README.md", "content": "# Documentation", "reason": "Update docs"}
                """);

        ModelAction action = parser.parseFromToolCall(tc);
        assertNotNull(action);
        assertEquals(ActionType.WRITE_FILE, action.getAction());
        assertEquals("README.md", action.getPath());
        assertEquals("# Documentation", action.getContent());
    }

    @Test
    void testParseFromToolCallSearchTool() {
        ToolCall tc = new ToolCall("call_2", "search_tool", """
                {"query": "password_hash", "reason": "Find hash method"}
                """);

        ModelAction action = parser.parseFromToolCall(tc);
        assertNotNull(action);
        assertEquals(ActionType.SEARCH, action.getAction());
        assertEquals("password_hash", action.getQuery());
    }

    @Test
    void testParseFromToolCallTerminalTool() {
        ToolCall tc = new ToolCall("call_3", "terminal_tool", """
                {"command": "echo 'build clean'", "reason": "Check output"}
                """);

        ModelAction action = parser.parseFromToolCall(tc);
        assertNotNull(action);
        assertEquals(ActionType.RUN_COMMAND, action.getAction());
        assertEquals("echo 'build clean'", action.getCommand());
    }

    @Test
    void testParseFromToolCallTestTool() {
        ToolCall tc = new ToolCall("call_4", "test_tool", """
                {"command": "mvn test", "reason": "Run test suite"}
                """);

        ModelAction action = parser.parseFromToolCall(tc);
        assertNotNull(action);
        assertEquals(ActionType.RUN_TESTS, action.getAction());
        assertEquals("mvn test", action.getCommand());
    }

    @Test
    void testParseFromToolCallGitTool() {
        ToolCall tcDiff = new ToolCall("call_5", "git_tool", "{\"action\": \"diff\"}");
        ModelAction actionDiff = parser.parseFromToolCall(tcDiff);
        assertNotNull(actionDiff);
        assertEquals(ActionType.GIT_DIFF, actionDiff.getAction());

        ToolCall tcStatus = new ToolCall("call_6", "git_tool", "{\"action\": \"status\"}");
        ModelAction actionStatus = parser.parseFromToolCall(tcStatus);
        assertNotNull(actionStatus);
        assertEquals(ActionType.GIT_STATUS, actionStatus.getAction());
    }

    @Test
    void testParseFromToolCallUnknownReturnsNull() {
        ToolCall tcUnknown = new ToolCall("call_7", "unknown_tool", "{}");
        assertNull(parser.parseFromToolCall(tcUnknown));
        assertNull(parser.parseFromToolCall(null));
    }
}
