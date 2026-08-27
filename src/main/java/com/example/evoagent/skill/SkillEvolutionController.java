package com.example.evoagent.skill;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills/evolution")
public class SkillEvolutionController {

    private final FailureAnalyzerService failureAnalyzerService;
    private final SkillGeneratorService skillGeneratorService;
    private final SkillAutoActivationService skillAutoActivationService;

    public SkillEvolutionController(
            FailureAnalyzerService failureAnalyzerService,
            SkillGeneratorService skillGeneratorService,
            SkillAutoActivationService skillAutoActivationService
    ) {
        this.failureAnalyzerService = failureAnalyzerService;
        this.skillGeneratorService = skillGeneratorService;
        this.skillAutoActivationService = skillAutoActivationService;
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
}
