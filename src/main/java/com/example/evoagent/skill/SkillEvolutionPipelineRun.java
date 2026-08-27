package com.example.evoagent.skill;

import com.example.evoagent.evaluation.EvaluationMetrics;

import java.time.Instant;
import java.util.List;

public record SkillEvolutionPipelineRun(
        String id,
        String status,
        String baselineRunId,
        String finalRunId,
        int analyzedFailureCount,
        int generatedSkillCount,
        int activatedSkillCount,
        int rejectedSkillCount,
        EvaluationMetrics beforeMetrics,
        EvaluationMetrics afterMetrics,
        FailureAnalysisResult analysis,
        SkillGenerationResult generation,
        List<SkillActivationDecision> activationDecisions,
        Instant startedAt,
        Instant finishedAt
) {
}
