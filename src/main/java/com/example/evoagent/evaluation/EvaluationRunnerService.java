package com.example.evoagent.evaluation;

import com.example.evoagent.agent.DeepSeekCodeReviewAgent;
import com.example.evoagent.agent.DeepSeekReviewResponse;
import com.example.evoagent.review.Finding;
import com.example.evoagent.rag.ReviewContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EvaluationRunnerService {

    private static final String DEFAULT_DATASET_NAME = "java-pr-review-mvp-10";
    private static final String AGENT_NAME = "DeepSeekCodeReviewAgent";

    private final EvaluationDatasetService datasetService;
    private final DeepSeekCodeReviewAgent reviewAgent;
    private final EvaluationFindingMatcher findingMatcher;
    private final EvaluationRunRepository runRepository;

    public EvaluationRunnerService(
            EvaluationDatasetService datasetService,
            DeepSeekCodeReviewAgent reviewAgent,
            EvaluationFindingMatcher findingMatcher,
            EvaluationRunRepository runRepository
    ) {
        this.datasetService = datasetService;
        this.reviewAgent = reviewAgent;
        this.findingMatcher = findingMatcher;
        this.runRepository = runRepository;
    }

    public EvaluationRun runAllCases() {
        return runCases(datasetService.loadCases(), DEFAULT_DATASET_NAME);
    }

    public EvaluationRun runSingleCase(String caseId) {
        EvaluationCase evaluationCase = datasetService.findCase(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation case not found: " + caseId));
        return runCases(List.of(evaluationCase), DEFAULT_DATASET_NAME + ":" + caseId);
    }

    private EvaluationRun runCases(List<EvaluationCase> evaluationCases, String datasetName) {
        String runId = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();
        EvaluationRun pendingRun = new EvaluationRun(
                runId,
                EvaluationRunStatus.PENDING,
                datasetName,
                AGENT_NAME,
                null,
                emptyMetrics(),
                List.of(),
                createdAt,
                null,
                null
        );
        runRepository.save(pendingRun);

        Instant startedAt = Instant.now();
        try {
            List<EvaluationCaseResult> caseResults = evaluationCases.stream()
                    .map(this::runCase)
                    .toList();
            EvaluationMetrics metrics = calculateMetrics(caseResults);
            EvaluationRun finishedRun = new EvaluationRun(
                    runId,
                    EvaluationRunStatus.SUCCEEDED,
                    datasetName,
                    AGENT_NAME,
                    null,
                    metrics,
                    caseResults,
                    createdAt,
                    startedAt,
                    Instant.now()
            );
            return runRepository.save(finishedRun);
        } catch (Exception e) {
            EvaluationRun failedRun = new EvaluationRun(
                    runId,
                    EvaluationRunStatus.FAILED,
                    datasetName,
                    AGENT_NAME,
                    e.getMessage(),
                    emptyMetrics(),
                    List.of(),
                    createdAt,
                    startedAt,
                    Instant.now()
            );
            return runRepository.save(failedRun);
        }
    }

    private EvaluationCaseResult runCase(EvaluationCase evaluationCase) {
        try {
            DeepSeekReviewResponse review = reviewAgent.review(
                    nullToList(evaluationCase.changedFiles()),
                    new ReviewContext(nullToList(evaluationCase.contextFiles()))
            );
            List<Finding> actualFindings = review.findings() == null ? List.of() : review.findings();
            List<ExpectedFinding> expectedFindings = nullToList(evaluationCase.expectedFindings());
            return compareFindings(evaluationCase, expectedFindings, actualFindings);
        } catch (Exception e) {
            return new EvaluationCaseResult(
                    evaluationCase.id(),
                    evaluationCase.title(),
                    EvaluationCaseStatus.ERROR,
                    nullToList(evaluationCase.expectedFindings()),
                    List.of(),
                    List.of(),
                    nullToList(evaluationCase.expectedFindings()),
                    List.of(),
                    e.getMessage()
            );
        }
    }

    private EvaluationCaseResult compareFindings(
            EvaluationCase evaluationCase,
            List<ExpectedFinding> expectedFindings,
            List<Finding> actualFindings
    ) {
        List<ExpectedFinding> matchedFindings = new ArrayList<>();
        List<ExpectedFinding> missedFindings = new ArrayList<>();
        Set<Integer> matchedActualIndexes = new HashSet<>();

        for (ExpectedFinding expected : expectedFindings) {
            int matchedIndex = findMatchingActual(expected, actualFindings, matchedActualIndexes);
            if (matchedIndex >= 0) {
                matchedFindings.add(expected);
                matchedActualIndexes.add(matchedIndex);
            } else {
                missedFindings.add(expected);
            }
        }

        List<Finding> unexpectedFindings = new ArrayList<>();
        for (int index = 0; index < actualFindings.size(); index++) {
            if (!matchedActualIndexes.contains(index)) {
                unexpectedFindings.add(actualFindings.get(index));
            }
        }

        EvaluationCaseStatus status = missedFindings.isEmpty() && unexpectedFindings.isEmpty()
                ? EvaluationCaseStatus.PASSED
                : EvaluationCaseStatus.FAILED;

        return new EvaluationCaseResult(
                evaluationCase.id(),
                evaluationCase.title(),
                status,
                expectedFindings,
                actualFindings,
                matchedFindings,
                missedFindings,
                unexpectedFindings,
                null
        );
    }

    private int findMatchingActual(
            ExpectedFinding expected,
            List<Finding> actualFindings,
            Set<Integer> matchedActualIndexes
    ) {
        for (int index = 0; index < actualFindings.size(); index++) {
            if (!matchedActualIndexes.contains(index)
                    && findingMatcher.matches(expected, actualFindings.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private EvaluationMetrics calculateMetrics(List<EvaluationCaseResult> results) {
        int totalCases = results.size();
        int passedCases = (int) results.stream()
                .filter(result -> result.status() == EvaluationCaseStatus.PASSED)
                .count();
        int errorCases = (int) results.stream()
                .filter(result -> result.status() == EvaluationCaseStatus.ERROR)
                .count();
        int failedCases = totalCases - passedCases - errorCases;

        int expectedFindingCount = results.stream()
                .mapToInt(result -> result.expectedFindings().size())
                .sum();
        int actualFindingCount = results.stream()
                .mapToInt(result -> result.actualFindings().size())
                .sum();
        int matchedFindingCount = results.stream()
                .mapToInt(result -> result.matchedFindings().size())
                .sum();
        int missedFindingCount = results.stream()
                .mapToInt(result -> result.missedFindings().size())
                .sum();
        int unexpectedFindingCount = results.stream()
                .mapToInt(result -> result.unexpectedFindings().size())
                .sum();

        int highRiskExpectedCount = results.stream()
                .flatMap(result -> result.expectedFindings().stream())
                .filter(finding -> "HIGH".equalsIgnoreCase(finding.level()))
                .toList()
                .size();
        int highRiskMatchedCount = results.stream()
                .flatMap(result -> result.matchedFindings().stream())
                .filter(finding -> "HIGH".equalsIgnoreCase(finding.level()))
                .toList()
                .size();

        double precision = divide(matchedFindingCount, actualFindingCount);
        double recall = divide(matchedFindingCount, expectedFindingCount);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        double highRiskRecall = divide(highRiskMatchedCount, highRiskExpectedCount);

        return new EvaluationMetrics(
                totalCases,
                passedCases,
                failedCases,
                errorCases,
                expectedFindingCount,
                actualFindingCount,
                matchedFindingCount,
                missedFindingCount,
                unexpectedFindingCount,
                precision,
                recall,
                f1,
                highRiskRecall
        );
    }

    private EvaluationMetrics emptyMetrics() {
        return new EvaluationMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private double divide(int numerator, int denominator) {
        if (denominator == 0) {
            return 0;
        }
        return (double) numerator / denominator;
    }

    private <T> List<T> nullToList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
