package com.example.evoagent.review;

import com.example.evoagent.github.PullRequestFile;

import java.time.Instant;
import java.util.List;

public record ReviewReport(
        String repo,
        int prNumber,
        String status,
        String summary,
        int changedFileCount,
        int totalAdditions,
        int totalDeletions,
        List<PullRequestFile> changedFiles,
        List<Finding> findings,
        Instant createdAt
) {
}
