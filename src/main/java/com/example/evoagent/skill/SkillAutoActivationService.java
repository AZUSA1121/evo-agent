package com.example.evoagent.skill;

import com.example.evoagent.evaluation.EvaluationMetrics;
import com.example.evoagent.evaluation.EvaluationRun;
import com.example.evoagent.evaluation.EvaluationRunnerService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SkillAutoActivationService {

    private static final double MAX_PRECISION_DROP = 0.05;

    private final AgentSkillRepository skillRepository;
    private final AgentSkillService skillService;
    private final EvaluationRunnerService evaluationRunnerService;
    private final SkillPromptService skillPromptService;

    public SkillAutoActivationService(
            AgentSkillRepository skillRepository,
            AgentSkillService skillService,
            EvaluationRunnerService evaluationRunnerService,
            SkillPromptService skillPromptService
    ) {
        this.skillRepository = skillRepository;
        this.skillService = skillService;
        this.evaluationRunnerService = evaluationRunnerService;
        this.skillPromptService = skillPromptService;
    }

    public SkillActivationDecision evaluateAndMaybeActivate(String skillId) {
        AgentSkill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillId));
        if (skill.status() != SkillStatus.CANDIDATE) {
            throw new IllegalStateException("Only CANDIDATE skills can be evaluated for auto activation: " + skillId);
        }

        EvaluationRun baselineRun = runScopedEvaluation(skill);
        EvaluationRun candidateRun = skillPromptService.withTemporarySkills(
                List.of(skill),
                () -> runScopedEvaluation(skill)
        );

        boolean shouldActivate = shouldActivate(baselineRun.metrics(), candidateRun.metrics());
        String reason = buildReason(baselineRun.metrics(), candidateRun.metrics(), shouldActivate);
        if (shouldActivate) {
            skillService.activate(skillId);
        }

        return new SkillActivationDecision(
                skill.id(),
                skill.name(),
                shouldActivate,
                reason,
                baselineRun.id(),
                candidateRun.id(),
                baselineRun.metrics(),
                candidateRun.metrics(),
                Instant.now()
        );
    }

    private EvaluationRun runScopedEvaluation(AgentSkill skill) {
        if (skill.sourceCaseId() != null && !skill.sourceCaseId().isBlank()) {
            return evaluationRunnerService.runSingleCase(skill.sourceCaseId());
        }
        return evaluationRunnerService.runAllCases();
    }

    private boolean shouldActivate(EvaluationMetrics baseline, EvaluationMetrics candidate) {
        if (candidate.errorCases() > 0) {
            return false;
        }

        boolean recallNotWorse = candidate.recall() >= baseline.recall();
        boolean highRiskRecallNotWorse = candidate.highRiskRecall() >= baseline.highRiskRecall();
        boolean precisionAcceptable = candidate.precision() >= baseline.precision() - MAX_PRECISION_DROP;
        boolean f1ImprovedOrEqual = candidate.f1() >= baseline.f1();
        boolean fixedMoreCases = candidate.passedCases() >= baseline.passedCases();

        return recallNotWorse
                && highRiskRecallNotWorse
                && precisionAcceptable
                && f1ImprovedOrEqual
                && fixedMoreCases;
    }

    private String buildReason(
            EvaluationMetrics baseline,
            EvaluationMetrics candidate,
            boolean activated
    ) {
        String decision = activated ? "activated" : "rejected";
        return "%s because baseline(f1=%.3f, precision=%.3f, recall=%.3f, highRiskRecall=%.3f) -> candidate(f1=%.3f, precision=%.3f, recall=%.3f, highRiskRecall=%.3f)"
                .formatted(
                        decision,
                        baseline.f1(),
                        baseline.precision(),
                        baseline.recall(),
                        baseline.highRiskRecall(),
                        candidate.f1(),
                        candidate.precision(),
                        candidate.recall(),
                        candidate.highRiskRecall()
                );
    }
}
