package com.example.codingagent.agent;

import com.example.codingagent.config.AgentProperties;
import com.example.codingagent.tools.GitTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentControllerTest {

    private AgentLoop agentLoop;
    private AgentProperties properties;
    private Executor directExecutor;
    private GitTool gitTool;
    private AgentController controller;

    @BeforeEach
    void setUp() {
        agentLoop = Mockito.mock(AgentLoop.class);
        properties = new AgentProperties();
        properties.setWorkspaceRoot(".");
        properties.setMaxAttempts(3);

        // Synchronous executor for unit testing
        directExecutor = Runnable::run;
        gitTool = Mockito.mock(GitTool.class);

        controller = new AgentController(agentLoop, properties, directExecutor, gitTool);
    }

    @Test
    void testCreateTaskDispatchesLoop() {
        AgentState state = controller.createTask("Fix payment bug", ".");

        assertNotNull(state);
        assertNotNull(state.getTaskId());
        assertEquals("Fix payment bug", state.getTask());
        assertEquals(3, state.getMaxAttempts());

        verify(agentLoop, times(1)).run(eq(state), any());
    }

    @Test
    void testCancelTask() {
        AgentState state = controller.createTask("Long task", ".");
        state.setStatus(AgentStatus.EXECUTING);

        boolean cancelled = controller.cancelTask(state.getTaskId());

        assertTrue(cancelled);
        assertEquals(AgentStatus.CANCELLED, state.getStatus());
    }
}
