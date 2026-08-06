package com.example.evoagent.webhook;

import com.example.evoagent.review.ReviewReport;

public record GitHubWebhookResponse(
        String status,
        String message,
        String event,
        String action,
        String repository,
        Integer prNumber,
        boolean commentPosted,
        ReviewReport report
) {
    public static GitHubWebhookResponse ignored(String event) {
        return new GitHubWebhookResponse(
                "ignored",
                "Ignore non pull_request event",
                event,
                null,
                null,
                null,
                false,
                null
        );
    }
}
