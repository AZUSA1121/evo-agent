package com.example.evoagent.skill;

import com.example.evoagent.evaluation.EvaluationCaseResult;
import com.example.evoagent.evaluation.EvaluationCaseStatus;
import com.example.evoagent.evaluation.EvaluationRun;
import com.example.evoagent.evaluation.EvaluationRunRepository;
import com.example.evoagent.evaluation.ExpectedFinding;
import com.example.evoagent.review.Finding;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FailureAnalyzerService {

    private final EvaluationRunRepository runRepository;

    public FailureAnalyzerService(EvaluationRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public FailureAnalysisResult analyze(String runId) {
        EvaluationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation run not found: " + runId));

        List<FailureAnalysisItem> items = new ArrayList<>();
        for (EvaluationCaseResult caseResult : nullToList(run.caseResults())) {
            analyzeMissedFindings(caseResult, items);
            analyzeUnexpectedFindings(caseResult, items);
            analyzeCaseError(caseResult, items);
        }

        Map<FailureType, Long> failureCounts = items.stream()
                .collect(Collectors.groupingBy(FailureAnalysisItem::failureType, Collectors.counting()));

        int totalCases = run.metrics() == null ? nullToList(run.caseResults()).size() : run.metrics().totalCases();
        int failedCases = run.metrics() == null ? countStatus(run, EvaluationCaseStatus.FAILED) : run.metrics().failedCases();
        int errorCases = run.metrics() == null ? countStatus(run, EvaluationCaseStatus.ERROR) : run.metrics().errorCases();

        return new FailureAnalysisResult(
                run.id(),
                totalCases,
                failedCases,
                errorCases,
                items.size(),
                failureCounts,
                items,
                Instant.now()
        );
    }

    private void analyzeMissedFindings(
            EvaluationCaseResult caseResult,
            List<FailureAnalysisItem> items
    ) {
        for (ExpectedFinding missed : nullToList(caseResult.missedFindings())) {
            items.add(new FailureAnalysisItem(
                    FailureType.MISSED_EXPECTED_FINDING,
                    caseResult.caseId(),
                    caseResult.title(),
                    missed.file(),
                    missed.type(),
                    missed.level(),
                    missed.title(),
                    "Agent missed an expected finding. This is a recall failure and should be converted into a review skill.",
                    buildSkillName(missed.type(), missed.title()),
                    missed.type(),
                    "Teach the review agent to detect: " + missed.title(),
                    nullToList(missed.keywords())
            ));
        }
    }

    private void analyzeUnexpectedFindings(
            EvaluationCaseResult caseResult,
            List<FailureAnalysisItem> items
    ) {
        for (Finding unexpected : nullToList(caseResult.unexpectedFindings())) {
            items.add(new FailureAnalysisItem(
                    FailureType.UNEXPECTED_FINDING,
                    caseResult.caseId(),
                    caseResult.title(),
                    unexpected.file(),
                    unexpected.type(),
                    unexpected.level(),
                    unexpected.title(),
                    "Agent produced a finding that was not expected by the dataset. This may be a false positive or a missing label.",
                    buildSkillName(unexpected.type(), "False Positive Guard: " + unexpected.title()),
                    unexpected.type(),
                    "Clarify when this pattern should be reported and when it should be ignored.",
                    List.of(unexpected.title(), unexpected.evidence(), unexpected.suggestion()).stream()
                            .filter(value -> value != null && !value.isBlank())
                            .toList()
            ));
        }
    }

    private void analyzeCaseError(
            EvaluationCaseResult caseResult,
            List<FailureAnalysisItem> items
    ) {
        if (caseResult.status() != EvaluationCaseStatus.ERROR) {
            return;
        }

        items.add(new FailureAnalysisItem(
                FailureType.CASE_EXECUTION_ERROR,
                caseResult.caseId(),
                caseResult.title(),
                null,
                "EVALUATION",
                "HIGH",
                "Evaluation case execution failed",
                caseResult.errorMessage(),
                "Evaluation Execution Stability Skill",
                "RELIABILITY",
                "Improve evaluation execution stability and error handling for this case.",
                caseResult.errorMessage() == null ? List.of() : List.of(caseResult.errorMessage())
        ));
    }

    private String buildSkillName(String category, String title) {
        String safeCategory = category == null || category.isBlank() ? "GENERAL" : category;
        String safeTitle = title == null || title.isBlank() ? "Review Improvement" : title;
        return safeCategory + " Skill - " + safeTitle;
    }

    private int countStatus(EvaluationRun run, EvaluationCaseStatus status) {
        return (int) nullToList(run.caseResults()).stream()
                .filter(result -> result.status() == status)
                .count();
    }

    private <T> List<T> nullToList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
