package com.example.evoagent.evaluation;

import java.util.List;

public record ExpectedFinding(
        String id,
        String file,
        String type,
        String level,
        String title,
        List<String> keywords
) {
}
