package com.example.evoagent.rag;

import java.util.List;

public record ReviewContext(
        List<CodeContext> codeContexts
) {
}
