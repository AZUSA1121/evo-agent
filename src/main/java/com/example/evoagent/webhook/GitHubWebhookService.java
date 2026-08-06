package com.example.evoagent.webhook;

import com.example.evoagent.github.GitHubClient;
import com.example.evoagent.report.ReviewMarkdownGenerator;
import com.example.evoagent.review.ReviewReport;
import com.example.evoagent.review.ReviewService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GitHubWebhookService {

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
            String action = text(root, "action");
            String fullName = root.path("repository").path("full_name").asText(null);
            int prNumber = root.path("pull_request").path("number").asInt();

            if (!shouldReview(action)) {
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
            ReviewReport report = reviewService.reviewGitHubPullRequest(repoParts[0], repoParts[1], prNumber);
            String markdown = markdownGenerator.generate(report);
            gitHubClient.createPullRequestComment(repoParts[0], repoParts[1], prNumber, markdown);

            return new GitHubWebhookResponse(
                    "REVIEWED",
                    "Pull request review completed and comment posted.",
                    event,
                    action,
                    fullName,
                    prNumber,
                    true,
                    report
            );
        } catch (Exception e) {
            if (e instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Failed to parse GitHub webhook payload", e);
        }
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
