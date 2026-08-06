package com.example.evoagent.agent;

import com.example.evoagent.review.Finding;

import java.util.List;

public record DeepSeekReviewResponse(
        String summary,
        List<Finding> findings
) {
}
