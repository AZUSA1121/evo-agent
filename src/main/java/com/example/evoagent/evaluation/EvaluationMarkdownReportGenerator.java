package com.example.evoagent.evaluation;

import com.example.evoagent.review.Finding;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class EvaluationMarkdownReportGenerator {

    public String generate(EvaluationRun run) {
        StringBuilder markdown = new StringBuilder();
        EvaluationMetrics metrics = run.metrics();

        markdown.append("# EvoAgent Evaluation Report\n\n");
        markdown.append("## Run Summary\n\n");
        markdown.append("- Run ID: `").append(run.id()).append("`\n");
        markdown.append("- Dataset: `").append(run.datasetName()).append("`\n");
        markdown.append("- Agent: `").append(run.agentName()).append("`\n");
        markdown.append("- Status: `").append(run.status()).append("`\n");
        markdown.append("- Created At: `").append(formatInstant(run.createdAt())).append("`\n");
        markdown.append("- Started At: `").append(formatInstant(run.startedAt())).append("`\n");
        markdown.append("- Finished At: `").append(formatInstant(run.finishedAt())).append("`\n\n");

        if (run.errorMessage() != null && !run.errorMessage().isBlank()) {
            markdown.append("Error: ").append(run.errorMessage()).append("\n\n");
        }

        markdown.append("## Metrics\n\n");
        markdown.append("- Total Cases: `").append(metrics.totalCases()).append("`\n");
        markdown.append("- Passed Cases: `").append(metrics.passedCases()).append("`\n");
        markdown.append("- Failed Cases: `").append(metrics.failedCases()).append("`\n");
        markdown.append("- Error Cases: `").append(metrics.errorCases()).append("`\n");
        markdown.append("- Precision: `").append(percent(metrics.precision())).append("`\n");
        markdown.append("- Recall: `").append(percent(metrics.recall())).append("`\n");
        markdown.append("- F1: `").append(percent(metrics.f1())).append("`\n");
        markdown.append("- High Risk Recall: `").append(percent(metrics.highRiskRecall())).append("`\n");
        markdown.append("- Matched Findings: `").append(metrics.matchedFindingCount()).append("`\n");
        markdown.append("- Missed Findings: `").append(metrics.missedFindingCount()).append("`\n");
        markdown.append("- Unexpected Findings: `").append(metrics.unexpectedFindingCount()).append("`\n\n");

        appendCaseResults(markdown, run.caseResults());
        return markdown.toString();
    }

    private void appendCaseResults(StringBuilder markdown, List<EvaluationCaseResult> caseResults) {
        markdown.append("## Case Results\n\n");
        if (caseResults == null || caseResults.isEmpty()) {
            markdown.append("No case results recorded.\n");
            return;
        }

        for (EvaluationCaseResult result : caseResults) {
            markdown.append("### ").append(result.caseId()).append(" - ").append(result.title()).append("\n\n");
            markdown.append("- Status: `").append(result.status()).append("`\n");
            markdown.append("- Expected Findings: `").append(result.expectedFindings().size()).append("`\n");
            markdown.append("- Actual Findings: `").append(result.actualFindings().size()).append("`\n");
            markdown.append("- Matched: `").append(result.matchedFindings().size()).append("`\n");
            markdown.append("- Missed: `").append(result.missedFindings().size()).append("`\n");
            markdown.append("- Unexpected: `").append(result.unexpectedFindings().size()).append("`\n\n");

            appendExpectedFindings(markdown, "Missed Findings", result.missedFindings());
            appendActualFindings(markdown, "Unexpected Findings", result.unexpectedFindings());

            if (result.errorMessage() != null && !result.errorMessage().isBlank()) {
                markdown.append("Error: ").append(result.errorMessage()).append("\n\n");
            }
        }
    }

    private void appendExpectedFindings(
            StringBuilder markdown,
            String heading,
            List<ExpectedFinding> findings
    ) {
        if (findings == null || findings.isEmpty()) {
            return;
        }

        markdown.append("#### ").append(heading).append("\n\n");
        for (ExpectedFinding finding : findings) {
            markdown.append("- `").append(finding.level()).append("` ")
                    .append(finding.title())
                    .append(" in `").append(finding.file()).append("`")
                    .append(" (`").append(finding.type()).append("`)\n");
        }
        markdown.append("\n");
    }

    private void appendActualFindings(
            StringBuilder markdown,
            String heading,
            List<Finding> findings
    ) {
        if (findings == null || findings.isEmpty()) {
            return;
        }

        markdown.append("#### ").append(heading).append("\n\n");
        for (Finding finding : findings) {
            markdown.append("- `").append(finding.level()).append("` ")
                    .append(finding.title())
                    .append(" in `").append(finding.file()).append("`")
                    .append(" (`").append(finding.type()).append("`)\n");
        }
        markdown.append("\n");
    }

    private String percent(double value) {
        return "%.1f%%".formatted(value * 100);
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "N/A" : instant.toString();
    }
}
