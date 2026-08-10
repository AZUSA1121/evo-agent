package com.example.evoagent.runtime;

import jakarta.validation.constraints.NotNull;

public record FailureInjectionRequest(
        @NotNull RuntimeNodeName nodeName,
        boolean enabled,
        String message
) {
}
