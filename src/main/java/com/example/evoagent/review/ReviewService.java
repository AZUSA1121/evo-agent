package com.example.evoagent.review;

import com.example.evoagent.github.GitHubClient;
import com.example.evoagent.github.PullRequestFile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ReviewService {

    private final GitHubClient gitHubClient;

    public ReviewService(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    public ReviewReport reviewGitHubPullRequest(String owner, String repo, int prNumber) {
        List<PullRequestFile> files = gitHubClient.getPullRequestFiles(owner, repo, prNumber);

        int totalAdditions = files.stream()
                .map(PullRequestFile::additions)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        int totalDeletions = files.stream()
                .map(PullRequestFile::deletions)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        String repoFullName = owner + "/" + repo;
        String summary = "PR #%d in %s contains %d changed files, %d additions, and %d deletions."
                .formatted(prNumber, repoFullName, files.size(), totalAdditions, totalDeletions);

        return new ReviewReport(
                repoFullName,
                prNumber,
                "READY_FOR_AGENT_REVIEW",
                summary,
                files.size(),
                totalAdditions,
                totalDeletions,
                files,
                List.of(),
                Instant.now()
        );
    }
}
