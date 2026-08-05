package com.example.evoagent.github;

public record PullRequestFile(
        String filename,
        String status,
        Integer additions,
        Integer deletions,
        Integer changes,
        String patch
) {
}
