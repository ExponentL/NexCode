package com.example.codingagent.controller;

import com.example.codingagent.agent.AgentController;
import com.example.codingagent.agent.AgentState;
import com.example.codingagent.logging.AgentEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentController agentController;

    @MockBean
    private AgentEventPublisher eventPublisher;

    @Test
    void testCreateTask() throws Exception {
        AgentState state = new AgentState("task-123", "Write unit test", "/repo", 5);
        when(agentController.createTask(anyString(), anyString())).thenReturn(state);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"task\": \"Write unit test\", \"repositoryPath\": \"/repo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value("task-123"))
                .andExpect(jsonPath("$.task").value("Write unit test"));
    }

    @Test
    void testGetTaskNotFound() throws Exception {
        when(agentController.getTask("unknown-id")).thenReturn(null);

        mockMvc.perform(get("/api/tasks/unknown-id"))
                .andExpect(status().isNotFound());
    }
}
