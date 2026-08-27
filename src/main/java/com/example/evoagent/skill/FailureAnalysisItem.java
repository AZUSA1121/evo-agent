package com.example.evoagent.skill;

import java.util.List;

public record FailureAnalysisItem(
        FailureType failureType,
        String caseId,
        String caseTitle,
        String file,
        String category,
        String level,
        String findingTitle,
        String summary,
        String recommendedSkillName,
        String recommendedSkillCategory,
        String recommendedSkillGoal,
        List<String> evidenceKeywords
) {
}
