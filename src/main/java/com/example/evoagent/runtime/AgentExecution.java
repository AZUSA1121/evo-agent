package com.example.evoagent.runtime;

import java.time.Instant;
import java.util.UUID;

public record AgentExecution(
        String id,
        String taskId,
        RuntimeNodeName nodeName,
        NodeStatus status,
        String input,
        String output,
        int retryCount,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs
) {
    public static AgentExecution start(String taskId, RuntimeNodeName nodeName, String input, int retryCount) {
        return new AgentExecution(
                UUID.randomUUID().toString(),
                taskId,
                nodeName,
                NodeStatus.RUNNING,
                input,
                null,
                retryCount,
                null,
                Instant.now(),
                null,
                null
        );
    }

    public AgentExecution succeed(String output) {
        Instant finishedAt = Instant.now();
        return new AgentExecution(
                id,
                taskId,
                nodeName,
                NodeStatus.SUCCEEDED,
                input,
                output,
                retryCount,
                null,
                startedAt,
                finishedAt,
                finishedAt.toEpochMilli() - startedAt.toEpochMilli()
        );
    }

    public AgentExecution fail(String errorMessage) {
        Instant finishedAt = Instant.now();
        return new AgentExecution(
                id,
                taskId,
                nodeName,
                NodeStatus.FAILED,
                input,
                output,
                retryCount,
                errorMessage,
                startedAt,
                finishedAt,
                finishedAt.toEpochMilli() - startedAt.toEpochMilli()
        );
    }
}
