package com.example.evoagent.runtime;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/failures")
    public FailureInjectionState configureFailure(@Valid @RequestBody FailureInjectionRequest request) {
        return failureInjector.configure(request);
    }

    @GetMapping("/failures")
    public Map<RuntimeNodeName, String> listFailures() {
        return failureInjector.activeFailures();
    }
}
