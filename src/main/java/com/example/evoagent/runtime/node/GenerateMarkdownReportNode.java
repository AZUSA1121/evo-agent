package com.example.evoagent.runtime.node;

import com.example.evoagent.report.ReviewMarkdownGenerator;
import com.example.evoagent.review.Finding;
import com.example.evoagent.review.ReviewReport;
import com.example.evoagent.runtime.AgentTask;
import com.example.evoagent.runtime.RuntimeContext;
import com.example.evoagent.runtime.RuntimeNode;
import com.example.evoagent.runtime.RuntimeNodeName;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class GenerateMarkdownReportNode implements RuntimeNode {

    private final ReviewMarkdownGenerator markdownGenerator;

    public GenerateMarkdownReportNode(ReviewMarkdownGenerator markdownGenerator) {
        this.markdownGenerator = markdownGenerator;
    }

    @Override
    public RuntimeNodeName name() {
        return RuntimeNodeName.GENERATE_MARKDOWN_REPORT;
    }

    @Override
    public String execute(AgentTask task, RuntimeContext context) {
        int totalAdditions = context.getPullRequestFiles().stream()
                .map(file -> file.additions())
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        int totalDeletions = context.getPullRequestFiles().stream()
                .map(file -> file.deletions())
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        List<String> contextFiles = context.getReviewContext() == null
                || context.getReviewContext().codeContexts() == null
                ? List.of()
                : context.getReviewContext().codeContexts().stream()
                .map(codeContext -> codeContext.path())
                .toList();
        List<Finding> findings = context.getAiReview().findings() == null
                ? List.of()
                : context.getAiReview().findings();

        String repoFullName = task.owner() + "/" + task.repo();
        String summary = "PR #%d in %s contains %d changed files, %d additions, and %d deletions."
                .formatted(task.prNumber(), repoFullName, context.getPullRequestFiles().size(), totalAdditions, totalDeletions);

        ReviewReport report = new ReviewReport(
                repoFullName,
                task.prNumber(),
                "REVIEWED_BY_RUNTIME_AGENT",
                summary,
                context.getPullRequestFiles().size(),
                totalAdditions,
                totalDeletions,
                context.getAiReview().summary(),
                context.getAiReview().riskLevel(),
                context.getAiReview().keyChanges(),
                context.getAiReview().testSuggestions(),
                contextFiles,
                context.getPullRequestFiles(),
                findings,
                Instant.now()
        );

        String markdown = markdownGenerator.generate(report);
        context.setReviewReport(report);
        context.setMarkdownReport(markdown);
        return "markdownLength=%d findings=%d".formatted(markdown.length(), findings.size());
    }
}
