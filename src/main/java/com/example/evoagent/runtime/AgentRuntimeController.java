package com.example.evoagent.runtime;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/runtime")
public class AgentRuntimeController {

    private final AgentRuntimeRepository repository;
    private final AgentWorkflowRuntime workflowRuntime;
    private final RuntimeFailureInjector failureInjector;

    public AgentRuntimeController(
            AgentRuntimeRepository repository,
            AgentWorkflowRuntime workflowRuntime,
            RuntimeFailureInjector failureInjector
    ) {
        this.repository = repository;
        this.workflowRuntime = workflowRuntime;
        this.failureInjector = failureInjector;
    }

    @PostMapping("/tasks")
    public AgentTask createTask(@Valid @RequestBody AgentTaskRequest request) {
        AgentTask task = AgentTask.create(
                request.owner(),
                request.repo(),
                request.prNumber(),
                request.eventType() == null || request.eventType().isBlank() ? "manual" : request.eventType()
        );
        return repository.saveTask(task);
    }

    @GetMapping("/tasks")
    public List<AgentTask> listTasks() {
        return repository.findAllTasks();
    }

    @GetMapping("/tasks/{taskId}")
    public AgentTask getTask(@PathVariable String taskId) {
        return repository.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    @GetMapping("/tasks/ref/{taskRef}")
    public AgentTask getTaskByRef(@PathVariable String taskRef) {
        return findTaskByRef(taskRef);
    }

    @PostMapping("/tasks/{taskId}/run")
    public AgentTask runTask(@PathVariable String taskId) {
        return workflowRuntime.runAsync(taskId);
    }

    @PostMapping("/tasks/{taskId}/retry")
    public AgentTask retryTask(@PathVariable String taskId) {
        return workflowRuntime.retryAsync(taskId);
    }

    @GetMapping("/tasks/{taskId}/executions")
    public List<AgentExecution> getExecutions(@PathVariable String taskId) {
        return repository.findExecutions(taskId);
    }

    @GetMapping("/tasks/{taskId}/trace")
    public List<AgentExecution> getTrace(@PathVariable String taskId) {
        return repository.findExecutions(taskId);
    }

    @GetMapping("/tasks/ref/{taskRef}/trace")
    public List<AgentExecution> getTraceByRef(@PathVariable String taskRef) {
        AgentTask task = findTaskByRef(taskRef);
        return repository.findExecutions(task.id());
    }

    @PostMapping("/failures")
    public FailureInjectionState configureFailure(@Valid @RequestBody FailureInjectionRequest request) {
        return failureInjector.configure(request);
    }

    @GetMapping("/failures")
    public Map<RuntimeNodeName, String> listFailures() {
        return failureInjector.activeFailures();
    }

    private AgentTask findTaskByRef(String taskRef) {
        if (taskRef == null || taskRef.length() < 8) {
            throw new ResponseStatusException(BAD_REQUEST, "Task ref must contain at least 8 characters.");
        }

        List<AgentTask> matches = repository.findTasksByIdPrefix(taskRef);
        if (matches.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Task not found for ref: " + taskRef);
        }
        if (matches.size() > 1) {
            throw new ResponseStatusException(BAD_REQUEST, "Task ref is ambiguous: " + taskRef);
        }
        return matches.get(0);
    }
}
