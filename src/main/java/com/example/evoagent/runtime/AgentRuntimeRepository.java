package com.example.evoagent.runtime;

import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AgentRuntimeRepository {

    private final Map<String, AgentTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AgentExecution>> executionsByTaskId = new ConcurrentHashMap<>();
    private final Map<String, RuntimeContext> checkpointsByTaskId = new ConcurrentHashMap<>();

    public AgentTask saveTask(AgentTask task) {
        tasks.put(task.id(), task);
        return task;
    }

    public Optional<AgentTask> findTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public List<AgentTask> findTasksByIdPrefix(String taskIdPrefix) {
        return tasks.values().stream()
                .filter(task -> task.id().startsWith(taskIdPrefix))
                .sorted(Comparator.comparing(AgentTask::createdAt).reversed())
                .toList();
    }

    public List<AgentTask> findAllTasks() {
        return tasks.values().stream()
                .sorted(Comparator.comparing(AgentTask::createdAt).reversed())
                .toList();
    }

    public AgentExecution saveExecution(AgentExecution execution) {
        Map<String, AgentExecution> executions = executionsByTaskId.computeIfAbsent(
                execution.taskId(),
                ignored -> new ConcurrentHashMap<>()
        );
        executions.put(execution.id(), execution);
        return execution;
    }

    public List<AgentExecution> findExecutions(String taskId) {
        Map<String, AgentExecution> executions = executionsByTaskId.get(taskId);
        if (executions == null) {
            return List.of();
        }
        return executions.values().stream()
                .sorted(Comparator.comparing(AgentExecution::startedAt))
                .toList();
    }

    public void saveCheckpoint(String taskId, RuntimeContext context) {
        checkpointsByTaskId.put(taskId, context.copy());
    }

    public Optional<RuntimeContext> findCheckpoint(String taskId) {
        RuntimeContext context = checkpointsByTaskId.get(taskId);
        if (context == null) {
            return Optional.empty();
        }
        return Optional.of(context.copy());
    }
}
