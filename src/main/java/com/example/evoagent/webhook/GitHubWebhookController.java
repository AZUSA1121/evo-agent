package com.example.evoagent.webhook;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhook")
public class GitHubWebhookController {

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

        if ("pull_request".equals(event)) {
            return webhookService.handle(event, payload);
        }

        return GitHubWebhookResponse.ignored(event);
//        signatureVerifier.verify(payload, signature);
//        return webhookService.handle(event, payload);
    }

    @PostMapping("/test")
    public String test(@RequestBody String body){

        System.out.println(body);

        return "ok";
    }
}
