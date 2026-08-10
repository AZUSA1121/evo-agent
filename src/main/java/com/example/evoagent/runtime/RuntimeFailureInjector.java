package com.example.evoagent.runtime;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RuntimeFailureInjector {

    private final Map<RuntimeNodeName, String> failures = new ConcurrentHashMap<>();

    public FailureInjectionState configure(FailureInjectionRequest request) {
        String message = request.message() == null || request.message().isBlank()
                ? "Injected failure for node " + request.nodeName()
                : request.message();

        if (request.enabled()) {
            failures.put(request.nodeName(), message);
        } else {
            failures.remove(request.nodeName());
        }

        return new FailureInjectionState(request.nodeName(), request.enabled(), message);
    }

    public Map<RuntimeNodeName, String> activeFailures() {
        return Map.copyOf(failures);
    }

    public void failIfEnabled(RuntimeNodeName nodeName) {
        String message = failures.remove(nodeName);
        if (message != null) {
            throw new IllegalStateException(message);
        }
    }
}
