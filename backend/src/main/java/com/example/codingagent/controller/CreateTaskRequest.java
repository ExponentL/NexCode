package com.example.codingagent.controller;

import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(
        @NotBlank(message = "Task description cannot be blank")
        String task,
        String repositoryPath
) {}
