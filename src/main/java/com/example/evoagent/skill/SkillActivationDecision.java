package com.example.evoagent.skill;

import com.example.evoagent.evaluation.EvaluationMetrics;

import java.time.Instant;

public record SkillActivationDecision(
        String skillId,
        String skillName,
        boolean activated,
        String reason,
        String baselineRunId,
        String candidateRunId,
        EvaluationMetrics baselineMetrics,
        EvaluationMetrics candidateMetrics,
        Instant decidedAt
) {
}
