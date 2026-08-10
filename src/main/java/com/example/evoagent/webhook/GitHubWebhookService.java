package com.example.evoagent.webhook;

import com.example.evoagent.runtime.AgentRuntimeRepository;
import com.example.evoagent.runtime.AgentTask;
import com.example.evoagent.runtime.AgentWorkflowRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GitHubWebhookService {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookService.class);

    private final ObjectMapper objectMapper;
    private final AgentRuntimeRepository runtimeRepository;
    private final AgentWorkflowRuntime workflowRuntime;

    public GitHubWebhookService(
            ObjectMapper objectMapper,
            AgentRuntimeRepository runtimeRepository,
            AgentWorkflowRuntime workflowRuntime
    ) {
        this.objectMapper = objectMapper;
        this.runtimeRepository = runtimeRepository;
        this.workflowRuntime = workflowRuntime;
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
                        null,
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
            AgentTask task = AgentTask.create(repoParts[0], repoParts[1], prNumber, "github:" + action);
            runtimeRepository.saveTask(task);
            workflowRuntime.runAsync(task.id());

            return new GitHubWebhookResponse(
                    "ACCEPTED",
                    "Pull request review task has been created and will run asynchronously.",
                    event,
                    action,
                    fullName,
                    prNumber,
                    task.id(),
                    false,
                    null
            );
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
