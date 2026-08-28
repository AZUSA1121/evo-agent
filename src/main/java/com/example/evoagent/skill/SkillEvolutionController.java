package com.example.evoagent.skill;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/skills/evolution")
public class SkillEvolutionController {

    private final FailureAnalyzerService failureAnalyzerService;
    private final SkillGeneratorService skillGeneratorService;
    private final SkillAutoActivationService skillAutoActivationService;
    private final SkillEvolutionPipelineService pipelineService;
    private final SkillEvolutionPipelineRunRepository pipelineRunRepository;

    public SkillEvolutionController(
            FailureAnalyzerService failureAnalyzerService,
            SkillGeneratorService skillGeneratorService,
            SkillAutoActivationService skillAutoActivationService,
            SkillEvolutionPipelineService pipelineService,
            SkillEvolutionPipelineRunRepository pipelineRunRepository
    ) {
        this.failureAnalyzerService = failureAnalyzerService;
        this.skillGeneratorService = skillGeneratorService;
        this.skillAutoActivationService = skillAutoActivationService;
        this.pipelineService = pipelineService;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    @GetMapping("/analysis")
    public FailureAnalysisResult analyzeFailures(@RequestParam String runId) {
        return failureAnalyzerService.analyze(runId);
    }

    @PostMapping("/generate")
    public SkillGenerationResult generateSkills(
            @RequestParam String runId,
            @RequestParam(defaultValue = "false") boolean includeUnexpectedFindings
    ) {
        return skillGeneratorService.generateFromRun(runId, includeUnexpectedFindings);
    }

    @PostMapping("/evaluate-and-activate")
    public SkillActivationDecision evaluateAndActivate(@RequestParam String skillId) {
        return skillAutoActivationService.evaluateAndMaybeActivate(skillId);
    }

    @PostMapping("/run")
    public SkillEvolutionPipelineRun runPipeline(
            @RequestParam(defaultValue = "true") boolean includeUnexpectedFindings
    ) {
        return pipelineService.run(includeUnexpectedFindings);
    }

    @GetMapping("/runs")
    public List<SkillEvolutionPipelineRun> listPipelineRuns() {
        return pipelineRunRepository.findAll();
    }

    @GetMapping("/runs/{runId}")
    public SkillEvolutionPipelineRun getPipelineRun(@PathVariable String runId) {
        return pipelineRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Skill evolution pipeline run not found: " + runId));
    }
}
