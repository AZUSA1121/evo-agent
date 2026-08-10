package com.example.evoagent.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AgentWorkflowRuntime {

    private static final Logger log = LoggerFactory.getLogger(AgentWorkflowRuntime.class);

    private final AgentRuntimeRepository repository;
    private final List<RuntimeNode> nodes;
    private final RuntimeFailureInjector failureInjector;
    private final Executor agentRuntimeExecutor;

    public AgentWorkflowRuntime(
            AgentRuntimeRepository repository,
            List<RuntimeNode> nodes,
            RuntimeFailureInjector failureInjector,
            Executor agentRuntimeExecutor
    ) {
        this.repository = repository;
        this.failureInjector = failureInjector;
        this.agentRuntimeExecutor = agentRuntimeExecutor;
        this.nodes = nodes.stream()
                .sorted(Comparator.comparingInt(node -> node.name().ordinal()))
                .toList();
    }

    public AgentTask runAsync(String taskId) {
        AgentTask task = getTask(taskId);
        CompletableFuture.runAsync(() -> run(taskId), agentRuntimeExecutor);
        return task;
    }

    public AgentTask run(String taskId) {
        AgentTask task = getTask(taskId);
        RuntimeContext context = new RuntimeContext();
        return runFrom(task, RuntimeNodeName.FETCH_PR_DIFF, context);
    }

    public AgentTask retryAsync(String taskId) {
        AgentTask task = getTask(taskId);
        validateRetryable(task);
        CompletableFuture.runAsync(() -> retry(taskId), agentRuntimeExecutor);
        return task;
    }

    public AgentTask retry(String taskId) {
        AgentTask task = getTask(taskId);
        validateRetryable(task);
        RuntimeContext context = repository.findCheckpoint(taskId)
                .orElseGet(RuntimeContext::new);
        RuntimeNodeName startNode = repository.findCheckpoint(taskId).isPresent()
                ? task.currentNode()
                : RuntimeNodeName.FETCH_PR_DIFF;
        log.info("Retrying task={} from node={} checkpointAvailable={}",
                task.id(), startNode, repository.findCheckpoint(taskId).isPresent());
        return runFrom(task, startNode, context);
    }

    private AgentTask runFrom(AgentTask task, RuntimeNodeName startNode, RuntimeContext context) {
        for (RuntimeNode node : nodes) {
            if (node.name().ordinal() < startNode.ordinal()) {
                continue;
            }
            task = repository.saveTask(task.markRunning(node.name()));
            AgentExecution execution = AgentExecution.start(
                    task.id(),
                    node.name(),
                    node.inputSummary(task, context),
                    retryCount(task.id(), node.name())
            );
            repository.saveExecution(execution);

            try {
                log.info("Running task={} node={}", task.id(), node.name());
                failureInjector.failIfEnabled(node.name());
                String output = node.execute(task, context);
                repository.saveExecution(execution.succeed(output));
                repository.saveCheckpoint(task.id(), context);
                log.info("Succeeded task={} node={}", task.id(), node.name());
            } catch (Exception e) {
                String errorMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                repository.saveExecution(execution.fail(errorMessage));
                task = repository.saveTask(task.markFailed(node.name(), errorMessage));
                log.error("Failed task={} node={}", task.id(), node.name(), e);
                return task;
            }
        }

        return repository.saveTask(task.markSucceeded());
    }

    private void validateRetryable(AgentTask task) {
        if (task.status() != TaskStatus.FAILED) {
            throw new IllegalStateException("Only FAILED tasks can be retried. taskId=%s status=%s"
                    .formatted(task.id(), task.status()));
        }
        if (task.currentNode() == null) {
            throw new IllegalStateException("Failed task has no current node. taskId=" + task.id());
        }
    }

    private AgentTask getTask(String taskId) {
        return repository.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    private int retryCount(String taskId, RuntimeNodeName nodeName) {
        return (int) repository.findExecutions(taskId).stream()
                .filter(execution -> execution.nodeName() == nodeName)
                .filter(execution -> execution.status() == NodeStatus.FAILED)
                .count();
    }
}
