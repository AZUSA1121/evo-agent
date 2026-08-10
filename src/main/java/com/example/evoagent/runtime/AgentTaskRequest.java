package com.example.evoagent.runtime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AgentTaskRequest(
        @NotBlank String owner,
        @NotBlank String repo,
        @Min(1) int prNumber,
        String eventType
) {
}
