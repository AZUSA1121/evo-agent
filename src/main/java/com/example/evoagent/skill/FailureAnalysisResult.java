package com.example.evoagent.skill;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FailureAnalysisResult(
        String runId,
        int totalCases,
        int failedCases,
        int errorCases,
        int failureItemCount,
        Map<FailureType, Long> failureCounts,
        List<FailureAnalysisItem> items,
        Instant analyzedAt
) {
}
