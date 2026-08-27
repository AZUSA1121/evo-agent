package com.example.evoagent.skill;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SkillGeneratorService {

    private final FailureAnalyzerService failureAnalyzerService;
    private final DeepSeekSkillGeneratorClient generatorClient;
    private final AgentSkillService skillService;

    public SkillGeneratorService(
            FailureAnalyzerService failureAnalyzerService,
            DeepSeekSkillGeneratorClient generatorClient,
            AgentSkillService skillService
    ) {
        this.failureAnalyzerService = failureAnalyzerService;
        this.generatorClient = generatorClient;
        this.skillService = skillService;
    }

    public SkillGenerationResult generateFromRun(String runId, boolean includeUnexpectedFindings) {
        FailureAnalysisResult analysis = failureAnalyzerService.analyze(runId);
        List<FailureAnalysisItem> failures = analysis.items().stream()
                .filter(item -> shouldGenerateSkill(item, includeUnexpectedFindings))
                .toList();

        List<AgentSkill> generatedSkills = failures.stream()
                .map(failure -> createCandidateSkill(runId, failure))
                .toList();

        return new SkillGenerationResult(
                runId,
                failures.size(),
                generatedSkills.size(),
                generatedSkills,
                Instant.now()
        );
    }

    private boolean shouldGenerateSkill(
            FailureAnalysisItem item,
            boolean includeUnexpectedFindings
    ) {
        if (item.failureType() == FailureType.MISSED_EXPECTED_FINDING
                || item.failureType() == FailureType.CASE_EXECUTION_ERROR) {
            return true;
        }
        return includeUnexpectedFindings && item.failureType() == FailureType.UNEXPECTED_FINDING;
    }

    private AgentSkill createCandidateSkill(String runId, FailureAnalysisItem failure) {
        GeneratedSkillDraft draft = generatorClient.generateSkill(failure);
        return skillService.createCandidate(new CreateSkillRequest(
                fallback(draft.name(), failure.recommendedSkillName()),
                fallback(draft.category(), failure.recommendedSkillCategory()),
                fallback(draft.description(), failure.recommendedSkillGoal()),
                fallback(draft.content(), buildFallbackContent(failure)),
                SkillSource.GENERATED,
                runId,
                failure.caseId()
        ));
    }

    private String buildFallbackContent(FailureAnalysisItem failure) {
        return """
                # %s

                ## When to apply
                Apply this skill when reviewing code related to `%s`.

                ## Detection rules
                - Look for evidence keywords: `%s`.

                ## Evidence to look for
                - File: `%s`
                - Failure type: `%s`

                ## Finding guidance
                Report this as `%s/%s` when evidence is clear.

                ## Suggested fix
                %s
                """.formatted(
                failure.recommendedSkillName(),
                failure.category(),
                failure.evidenceKeywords(),
                failure.file(),
                failure.failureType(),
                failure.category(),
                failure.level(),
                failure.recommendedSkillGoal()
        );
    }

    private String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
