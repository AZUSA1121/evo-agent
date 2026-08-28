package com.example.evoagent.skill;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentSkill(
        String id,
        String name,
        int version,
        SkillStatus status,
        SkillSource source,
        String category,
        String description,
        String content,
        String sourceRunId,
        String sourceCaseId,
        List<SkillEvaluationResult> evaluationResults,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt
) {

    public static AgentSkill candidate(CreateSkillRequest request) {
        Instant now = Instant.now();
        return new AgentSkill(
                UUID.randomUUID().toString(),
                request.name(),
                1,
                SkillStatus.CANDIDATE,
                request.source() == null ? SkillSource.MANUAL : request.source(),
                request.category(),
                request.description(),
                request.content(),
                request.sourceRunId(),
                request.sourceCaseId(),
                List.of(),
                now,
                now,
                null
        );
    }

    public AgentSkill withStatus(SkillStatus nextStatus) {
        Instant now = Instant.now();
        return new AgentSkill(
                id,
                name,
                version,
                nextStatus,
                source,
                category,
                description,
                content,
                sourceRunId,
                sourceCaseId,
                evaluationResults == null ? List.of() : evaluationResults,
                createdAt,
                now,
                nextStatus == SkillStatus.ACTIVE ? now : activatedAt
        );
    }
}
