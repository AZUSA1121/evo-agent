package com.example.evoagent.review;

import com.example.evoagent.github.PullRequestFile;

import java.time.Instant;
import java.util.List;

public record ReviewReportView(
        String id,
        String taskId,
        String taskRef,
        String repo,
        int prNumber,
        String status,
        String summary,
        int changedFileCount,
        int totalAdditions,
        int totalDeletions,
        String aiSummary,
        String riskLevel,
        List<String> keyChanges,
        List<String> testSuggestions,
        List<String> contextFiles,
        List<PullRequestFile> changedFiles,
        List<Finding> findings,
        String markdown,
        Instant createdAt
) {
}
