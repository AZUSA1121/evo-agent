package com.example.evoagent.review;

import com.example.evoagent.agent.DeepSeekReviewResponse;
import com.example.evoagent.agent.DeepSeekCodeReviewAgent;
import com.example.evoagent.github.GitHubClient;
import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.rag.CodeRagService;
import com.example.evoagent.rag.ReviewContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ReviewService {

    private final GitHubClient gitHubClient;
    private final DeepSeekCodeReviewAgent codeReviewAgent;
    private final CodeRagService codeRagService;

    public ReviewService(
            GitHubClient gitHubClient,
            DeepSeekCodeReviewAgent codeReviewAgent,
            CodeRagService codeRagService
    ) {
        this.gitHubClient = gitHubClient;
        this.codeReviewAgent = codeReviewAgent;
        this.codeRagService = codeRagService;
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
        ReviewContext reviewContext = codeRagService.retrieve(owner, repo, prNumber, files);
        List<String> contextFiles = reviewContext.codeContexts().stream()
                .map(context -> context.path())
                .toList();
        DeepSeekReviewResponse aiReview = codeReviewAgent.review(files, reviewContext);
        List<Finding> findings = aiReview.findings();
        String summary = "PR #%d in %s contains %d changed files, %d additions, and %d deletions."
                .formatted(prNumber, repoFullName, files.size(), totalAdditions, totalDeletions);

        return new ReviewReport(
                repoFullName,
                prNumber,
                "REVIEWED_BY_DEEPSEEK_AGENT",
                summary,
                files.size(),
                totalAdditions,
                totalDeletions,
                aiReview.summary(),
                aiReview.riskLevel(),
                aiReview.keyChanges(),
                aiReview.testSuggestions(),
                contextFiles,
                files,
                findings,
                Instant.now()
        );
    }
}
