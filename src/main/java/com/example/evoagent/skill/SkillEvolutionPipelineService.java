package com.example.evoagent.skill;

import com.example.evoagent.evaluation.EvaluationRun;
import com.example.evoagent.evaluation.EvaluationRunStatus;
import com.example.evoagent.evaluation.EvaluationRunnerService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SkillEvolutionPipelineService {

    private final EvaluationRunnerService evaluationRunnerService;
    private final FailureAnalyzerService failureAnalyzerService;
    private final SkillGeneratorService skillGeneratorService;
    private final SkillAutoActivationService skillAutoActivationService;
    private final SkillEvolutionPipelineRunRepository pipelineRunRepository;

    public SkillEvolutionPipelineService(
            EvaluationRunnerService evaluationRunnerService,
            FailureAnalyzerService failureAnalyzerService,
            SkillGeneratorService skillGeneratorService,
            SkillAutoActivationService skillAutoActivationService,
            SkillEvolutionPipelineRunRepository pipelineRunRepository
    ) {
        this.evaluationRunnerService = evaluationRunnerService;
        this.failureAnalyzerService = failureAnalyzerService;
        this.skillGeneratorService = skillGeneratorService;
        this.skillAutoActivationService = skillAutoActivationService;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    public SkillEvolutionPipelineRun run(boolean includeUnexpectedFindings) {
        String pipelineRunId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();

        EvaluationRun baselineRun = requireSucceeded(evaluationRunnerService.runAllCases(), "baseline evaluation");
        FailureAnalysisResult analysis = failureAnalyzerService.analyze(baselineRun.id());
        SkillGenerationResult generation = skillGeneratorService.generateFromRun(
                baselineRun.id(),
                includeUnexpectedFindings
        );
        if (generation.generatedSkillCount() == 0) {
            return pipelineRunRepository.save(new SkillEvolutionPipelineRun(
                    pipelineRunId,
                    "NO_SKILL_GENERATED",
                    baselineRun.id(),
                    baselineRun.id(),
                    analysis.failureItemCount(),
                    0,
                    0,
                    0,
                    baselineRun.metrics(),
                    baselineRun.metrics(),
                    analysis,
                    generation,
                    List.of(),
                    startedAt,
                    Instant.now()
            ));
        }

        List<SkillActivationDecision> decisions = generation.generatedSkills().stream()
                .map(skill -> skillAutoActivationService.evaluateAndMaybeActivate(skill.id()))
                .toList();
        EvaluationRun finalRun = requireSucceeded(evaluationRunnerService.runAllCases(), "final evaluation");

        int activatedSkillCount = (int) decisions.stream()
                .filter(SkillActivationDecision::activated)
                .count();
        int rejectedSkillCount = decisions.size() - activatedSkillCount;

        return pipelineRunRepository.save(new SkillEvolutionPipelineRun(
                pipelineRunId,
                "SUCCEEDED",
                baselineRun.id(),
                finalRun.id(),
                analysis.failureItemCount(),
                generation.generatedSkillCount(),
                activatedSkillCount,
                rejectedSkillCount,
                baselineRun.metrics(),
                finalRun.metrics(),
                analysis,
                generation,
                decisions,
                startedAt,
                Instant.now()
        ));
    }

    private EvaluationRun requireSucceeded(EvaluationRun run, String stageName) {
        if (run.status() != EvaluationRunStatus.SUCCEEDED) {
            throw new IllegalStateException("Skill evolution pipeline failed during " + stageName
                    + ": " + run.errorMessage());
        }
        return run;
    }
}
