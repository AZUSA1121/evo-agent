package com.example.evoagent.agent;

import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.rag.ReviewContext;
import com.example.evoagent.review.Finding;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeepSeekCodeReviewAgent {

    private final DeepSeekClient deepSeekClient;

    public DeepSeekCodeReviewAgent(DeepSeekClient deepSeekClient) {
        this.deepSeekClient = deepSeekClient;
    }

    public List<Finding> review(List<PullRequestFile> files, ReviewContext reviewContext) {
        String prompt = buildPrompt(files, reviewContext);
        DeepSeekReviewResponse response = deepSeekClient.reviewCode(prompt);
        if (response.findings() == null) {
            return List.of();
        }
        return response.findings();
    }

    private String buildPrompt(List<PullRequestFile> files, ReviewContext reviewContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                You are an enterprise Java code review agent.

                Review the GitHub pull request patch below, using the retrieved code context.

                Focus on:
                1. Security: SQL injection, permission bypass, hardcoded secrets, sensitive data leakage.
                2. Reliability: null pointer risks, swallowed exceptions, transaction issues, Redis/MQ consistency risks.
                3. Performance: SELECT *, N+1 queries, slow SQL risks, repeated database calls in loops.
                4. Code quality: production logging, maintainability, risky changes.

                Return only valid json in this exact shape:
                {
                  "summary": "short review summary",
                  "findings": [
                    {
                      "file": "path/to/File.java",
                      "line": 12,
                      "type": "SECURITY|RELIABILITY|PERFORMANCE|CODE_QUALITY",
                      "level": "HIGH|MEDIUM|LOW",
                      "title": "short issue title",
                      "evidence": "why this is risky",
                      "suggestion": "how to fix it"
                    }
                  ]
                }

                If no issue is found, return this json:
                {"summary":"No obvious issue found.","findings":[]}

                Retrieved code context:
                """);

        if (reviewContext != null && reviewContext.codeContexts() != null) {
            for (var context : reviewContext.codeContexts()) {
                prompt.append("\n\n--- CONTEXT FILE: ").append(context.path()).append(" ---\n");
                prompt.append(context.content());
            }
        }

        prompt.append("""

                Pull request patch:
                """);

        for (PullRequestFile file : files) {
            prompt.append("\n\n--- FILE: ").append(file.filename()).append(" ---\n");
            prompt.append("status: ").append(file.status()).append("\n");
            prompt.append("additions: ").append(file.additions()).append("\n");
            prompt.append("deletions: ").append(file.deletions()).append("\n");
            prompt.append(file.patch() == null ? "" : file.patch());
        }

        return prompt.toString();
    }
}
