package com.example.evoagent.webhook;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/webhook")
public class GitHubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

    private final GitHubWebhookService webhookService;
    private final GitHubWebhookSignatureVerifier signatureVerifier;

    public GitHubWebhookController(
            GitHubWebhookService webhookService,
            GitHubWebhookSignatureVerifier signatureVerifier
    ) {
        this.webhookService = webhookService;
        this.signatureVerifier = signatureVerifier;
    }

    @PostMapping("/github")
    public GitHubWebhookResponse handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256") String signature,
            @RequestBody String payload
    ) {

        signatureVerifier.verify(payload, signature);
        log.info("Received GitHub webhook event={}", event);

        if ("pull_request".equals(event)) {
            return webhookService.handle(event, payload);
        }

        log.info("Ignored GitHub webhook event={}", event);
        return GitHubWebhookResponse.ignored(event);
    }
}
