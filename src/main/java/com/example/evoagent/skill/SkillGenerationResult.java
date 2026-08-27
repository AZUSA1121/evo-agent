package com.example.evoagent.skill;

import java.time.Instant;
import java.util.List;

public record SkillGenerationResult(
        String runId,
        int analyzedFailureCount,
        int generatedSkillCount,
        List<AgentSkill> generatedSkills,
        Instant generatedAt
) {
}
