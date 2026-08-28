package com.example.evoagent.skill;

import java.time.Instant;

public record SkillEvaluationResult(
        String runId,
        double precision,
        double recall,
        double f1,
        double highRiskRecall,
        Instant evaluatedAt
) {
}
