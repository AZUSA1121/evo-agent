package com.example.evoagent.evaluation;

import com.example.evoagent.review.Finding;

import java.util.List;

public record EvaluationCaseResult(
        String caseId,
        String title,
        EvaluationCaseStatus status,
        List<ExpectedFinding> expectedFindings,
        List<Finding> actualFindings,
        List<ExpectedFinding> matchedFindings,
        List<ExpectedFinding> missedFindings,
        List<Finding> unexpectedFindings,
        String errorMessage
) {
}
