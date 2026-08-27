package com.example.evoagent.skill;

import jakarta.validation.constraints.NotBlank;

public record CreateSkillRequest(
        @NotBlank String name,
        String category,
        String description,
        @NotBlank String content,
        SkillSource source,
        String sourceRunId,
        String sourceCaseId
) {
}
