package com.example.evoagent.runtime;

public record FailureInjectionState(
        RuntimeNodeName nodeName,
        boolean enabled,
        String message,
        boolean oneShot
) {
}
