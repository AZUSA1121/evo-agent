package com.example.evoagent.evaluation;

public record EvaluationMetrics(
        int totalCases,
        int passedCases,
        int failedCases,
        int errorCases,
        int expectedFindingCount,
        int actualFindingCount,
        int matchedFindingCount,
        int missedFindingCount,
        int unexpectedFindingCount,
        double precision,
        double recall,
        double f1,
        double highRiskRecall
) {
}
