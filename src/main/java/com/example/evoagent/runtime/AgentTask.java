package com.example.evoagent.runtime;

import java.time.Instant;
import java.util.UUID;

public record AgentTask(
        String id,
        String owner,
        String repo,
        int prNumber,
        String eventType,
        TaskStatus status,
        RuntimeNodeName currentNode,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static AgentTask create(String owner, String repo, int prNumber, String eventType) {
        Instant now = Instant.now();
        return new AgentTask(
                UUID.randomUUID().toString(),
                owner,
                repo,
                prNumber,
                eventType,
                TaskStatus.PENDING,
                RuntimeNodeName.FETCH_PR_DIFF,
                null,
                now,
                now
        );
    }

    public AgentTask markRunning(RuntimeNodeName node) {
        return new AgentTask(id, owner, repo, prNumber, eventType, TaskStatus.RUNNING, node, null, createdAt, Instant.now());
    }

    public AgentTask markSucceeded() {
        return new AgentTask(id, owner, repo, prNumber, eventType, TaskStatus.SUCCEEDED, currentNode, null, createdAt, Instant.now());
    }

    public AgentTask markFailed(RuntimeNodeName node, String errorMessage) {
        return new AgentTask(id, owner, repo, prNumber, eventType, TaskStatus.FAILED, node, errorMessage, createdAt, Instant.now());
    }
}
