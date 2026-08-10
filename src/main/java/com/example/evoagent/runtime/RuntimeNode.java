package com.example.evoagent.runtime;

public interface RuntimeNode {

    RuntimeNodeName name();

    default String inputSummary(AgentTask task, RuntimeContext context) {
        return "repo=%s/%s pr=%d".formatted(task.owner(), task.repo(), task.prNumber());
    }

    String execute(AgentTask task, RuntimeContext context);
}
