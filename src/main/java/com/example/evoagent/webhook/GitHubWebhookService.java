package com.example.evoagent.webhook;

import com.example.evoagent.github.GitHubClient;
import com.example.evoagent.report.ReviewMarkdownGenerator;
import com.example.evoagent.review.ReviewReport;
import com.example.evoagent.review.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

@Service
public class GitHubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookService.class);

    private final ObjectMapper objectMapper;
    private final ReviewService reviewService;
    private final ReviewMarkdownGenerator markdownGenerator;
    private final GitHubClient gitHubClient;

    public GitHubWebhookService(
            ObjectMapper objectMapper,
            ReviewService reviewService,
            ReviewMarkdownGenerator markdownGenerator,
            GitHubClient gitHubClient
    ) {
        this.objectMapper = objectMapper;
        this.reviewService = reviewService;
        this.markdownGenerator = markdownGenerator;
        this.gitHubClient = gitHubClient;
    }

    public GitHubWebhookResponse handle(String event, String payload) {
        if (!"pull_request".equals(event)) {
            return new GitHubWebhookResponse(
                    "IGNORED",
                    "Only pull_request events are handled.",
                    event,
                    null,
                    null,
                    null,
                    false,
                    null
            );
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            return handlePullRequestEvent(event, root);
        } catch (Exception e) {
            if (e instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Failed to parse GitHub webhook payload", e);
        }
    }

    private GitHubWebhookResponse handlePullRequestEvent(String event, JsonNode root) {
            String action = text(root, "action");
            String fullName = root.path("repository").path("full_name").asText(null);
            int prNumber = root.path("pull_request").path("number").asInt();

            if (!shouldReview(action)) {
                log.info("Ignored pull_request webhook action={} repo={} pr={}", action, fullName, prNumber);
                return new GitHubWebhookResponse(
                        "IGNORED",
                        "Pull request action does not require review.",
                        event,
                        action,
                        fullName,
                        prNumber,
                        false,
                        null
                );
            }

            if (fullName == null || !fullName.contains("/")) {
                throw new IllegalStateException("GitHub webhook payload is missing repository.full_name");
            }
            if (prNumber <= 0) {
                throw new IllegalStateException("GitHub webhook payload is missing pull_request.number");
            }

            String[] repoParts = fullName.split("/", 2);
            log.info("Accepted pull_request webhook action={} repo={} pr={}", action, fullName, prNumber);
            runReviewAsync(repoParts[0], repoParts[1], prNumber);

            return new GitHubWebhookResponse(
                    "ACCEPTED",
                    "Pull request review has been accepted and will run asynchronously.",
                    event,
                    action,
                    fullName,
                    prNumber,
                    false,
                    null
            );
    }

    private void runReviewAsync(String owner, String repo, int prNumber) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting AI review for {}/{} PR #{}", owner, repo, prNumber);
                ReviewReport report = reviewService.reviewGitHubPullRequest(owner, repo, prNumber);
                log.info("Generated AI review for {}/{} PR #{} findings={}",
                        owner, repo, prNumber, report.findings().size());
                String markdown = markdownGenerator.generate(report);
                log.info("Posting AI review comment for {}/{} PR #{}", owner, repo, prNumber);
                gitHubClient.createPullRequestComment(owner, repo, prNumber, markdown);
                log.info("Posted AI review comment for {}/{} PR #{}", owner, repo, prNumber);
            } catch (Exception e) {
                log.error("Failed to review {}/{} PR #{}", owner, repo, prNumber, e);
            }
        });
    }

    private boolean shouldReview(String action) {
        return "opened".equals(action)
                || "synchronize".equals(action)
                || "reopened".equals(action);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }
}
