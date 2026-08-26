package com.example.evoagent.evaluation;

import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.rag.CodeContext;

import java.util.List;

public record EvaluationCase(
        String id,
        String title,
        String category,
        String description,
        String repo,
        int prNumber,
        List<PullRequestFile> changedFiles,
        List<CodeContext> contextFiles,
        List<ExpectedFinding> expectedFindings
) {
}
