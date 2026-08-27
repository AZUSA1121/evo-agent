package com.example.evoagent.evaluation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationDatasetService datasetService;
    private final EvaluationRunnerService runnerService;
    private final EvaluationRunRepository runRepository;
    private final EvaluationMarkdownReportGenerator reportGenerator;

    public EvaluationController(
            EvaluationDatasetService datasetService,
            EvaluationRunnerService runnerService,
            EvaluationRunRepository runRepository,
            EvaluationMarkdownReportGenerator reportGenerator
    ) {
        this.datasetService = datasetService;
        this.runnerService = runnerService;
        this.runRepository = runRepository;
        this.reportGenerator = reportGenerator;
    }

    @GetMapping("/cases")
    public List<EvaluationCase> listCases() {
        return datasetService.loadCases();
    }

    @GetMapping("/cases/{caseId}")
    public EvaluationCase getCase(@PathVariable String caseId) {
        return datasetService.findCase(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation case not found: " + caseId));
    }

    @PostMapping("/runs")
    public EvaluationRun runEvaluation(@RequestParam(required = false) String caseId) {
        if (caseId != null && !caseId.isBlank()) {
            return runnerService.runSingleCase(caseId);
        }
        return runnerService.runAllCases();
    }

    @GetMapping("/runs")
    public List<EvaluationRun> listRuns() {
        return runRepository.findAll();
    }

    @GetMapping("/runs/{runId}")
    public EvaluationRun getRun(@PathVariable String runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation run not found: " + runId));
    }

    @GetMapping(value = "/runs/{runId}/report", produces = "text/markdown;charset=UTF-8")
    public String getRunReport(@PathVariable String runId) {
        EvaluationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation run not found: " + runId));
        return reportGenerator.generate(run);
    }
}
