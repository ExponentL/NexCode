package com.example.codingagent.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentStateTest {

    @Test
    void testInitialStateAndUpdates() {
        AgentState state = new AgentState("task-1", "Fix bug in login", "/path/to/repo", 5);

        assertEquals("task-1", state.getTaskId());
        assertEquals("Fix bug in login", state.getTask());
        assertEquals(AgentStatus.IDLE, state.getStatus());
        assertEquals(0, state.getAttempts());
        assertEquals(5, state.getMaxAttempts());

        state.recordFileInspected("src/main/Auth.java");
        state.recordFileModified("src/main/Auth.java");
        state.recordCommandExecuted("terminal: mvn test");
        state.recordError("Sample error occurred");
        state.recordTestResult(TestResultItem.success("AuthTest", "testLogin()", 120));

        AgentStep step1 = new AgentStep(1, "Inspect Auth", "Read auth files", "inspect");
        step1.start();
        step1.complete("Indexed Auth files");
        state.addPlanStep(step1);

        assertEquals(1, state.getFilesInspected().size());
        assertEquals(1, state.getFilesModified().size());
        assertEquals(1, state.getCommandsExecuted().size());
        assertEquals(1, state.getErrors().size());
        assertEquals(1, state.getTestResults().size());
        assertEquals(1, state.getPlan().size());
        assertEquals(AgentStep.StepStatus.COMPLETED, state.getPlan().get(0).getStatus());
        assertTrue(state.getTestResults().get(0).passed());
    }
}
