package com.example.codingagent.context;

import java.util.List;

public record RepositoryContext(
        String repositoryPath,
        String repositoryName,
        String projectType,
        String readmeSummary,
        List<String> keyFiles,
        String gitStatus
) {}
