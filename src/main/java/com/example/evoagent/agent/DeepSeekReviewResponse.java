package com.example.evoagent.agent;

import com.example.evoagent.review.Finding;

import java.util.List;

public record DeepSeekReviewResponse(
        String summary,
        String riskLevel,
        List<String> keyChanges,
        List<String> testSuggestions,
        List<Finding> findings
) {
}
