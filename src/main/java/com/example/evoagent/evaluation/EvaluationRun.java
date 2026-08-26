package com.example.evoagent.evaluation;

import java.time.Instant;
import java.util.List;

public record EvaluationRun(
        String id,
        EvaluationRunStatus status,
        String datasetName,
        String agentName,
        String errorMessage,
        EvaluationMetrics metrics,
        List<EvaluationCaseResult> caseResults,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
}
