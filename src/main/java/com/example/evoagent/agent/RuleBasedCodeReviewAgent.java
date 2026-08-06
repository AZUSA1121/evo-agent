package com.example.evoagent.agent;

import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.review.Finding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedCodeReviewAgent {

    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*");

    public List<Finding> review(List<PullRequestFile> files) {
        List<Finding> findings = new ArrayList<>();
        for (PullRequestFile file : files) {
            if (file.patch() == null || file.patch().isBlank()) {
                continue;
            }
            reviewFile(file, findings);
        }
        return findings;
    }

    private void reviewFile(PullRequestFile file, List<Finding> findings) {
        String[] lines = file.patch().split("\\R");
        int newLineNumber = 0;

        for (int index = 0; index < lines.length; index++) {
            String patchLine = lines[index];
            Matcher matcher = HUNK_HEADER.matcher(patchLine);
            if (matcher.matches()) {
                newLineNumber = Integer.parseInt(matcher.group(1));
                continue;
            }

            if (patchLine.startsWith("+++") || patchLine.startsWith("---")) {
                continue;
            }

            if (patchLine.startsWith("+")) {
                String code = patchLine.substring(1).trim();
                detectIssues(file.filename(), newLineNumber, code, lines, index, findings);
                newLineNumber++;
            } else if (patchLine.startsWith(" ")) {
                newLineNumber++;
            }
        }
    }

    private void detectIssues(
            String filename,
            int line,
            String code,
            String[] patchLines,
            int index,
            List<Finding> findings
    ) {
        String lowerCode = code.toLowerCase(Locale.ROOT);

        if (code.contains("System.out.println")) {
            findings.add(new Finding(
                    filename,
                    line,
                    "CODE_QUALITY",
                    "LOW",
                    "Avoid console printing in application code",
                    "New code uses System.out.println, which is hard to control in production logs.",
                    "Use a logger such as Slf4j instead."
            ));
        }

        if (containsHardcodedSecret(lowerCode)) {
            findings.add(new Finding(
                    filename,
                    line,
                    "SECURITY",
                    "HIGH",
                    "Possible hardcoded secret",
                    "New code appears to define a password, token, secret, or API key inline.",
                    "Move secrets to environment variables or a secure secret manager."
            ));
        }

        if (code.contains("printStackTrace()")) {
            findings.add(new Finding(
                    filename,
                    line,
                    "RELIABILITY",
                    "MEDIUM",
                    "Exception is printed instead of handled",
                    "New code calls printStackTrace(), which may hide failures from normal logging and monitoring.",
                    "Log the exception with context and handle or rethrow it according to business needs."
            ));
        }

        if (lowerCode.contains("select *")) {
            findings.add(new Finding(
                    filename,
                    line,
                    "PERFORMANCE",
                    "MEDIUM",
                    "Avoid SELECT * in queries",
                    "New SQL selects all columns, which may fetch unnecessary data and make schema changes riskier.",
                    "Select only the columns needed by this code path."
            ));
        }

        if (code.startsWith("catch ") && isLikelyEmptyCatch(patchLines, index)) {
            findings.add(new Finding(
                    filename,
                    line,
                    "RELIABILITY",
                    "HIGH",
                    "Empty catch block",
                    "New code catches an exception without handling it, which can hide real failures.",
                    "Log the exception and either recover explicitly or rethrow a business exception."
            ));
        }
    }

    private boolean containsHardcodedSecret(String lowerCode) {
        boolean hasSecretName = lowerCode.contains("password")
                || lowerCode.contains("passwd")
                || lowerCode.contains("secret")
                || lowerCode.contains("token")
                || lowerCode.contains("api_key")
                || lowerCode.contains("apikey");
        boolean hasAssignedString = lowerCode.contains("=") && (lowerCode.contains("\"") || lowerCode.contains("'"));
        return hasSecretName && hasAssignedString;
    }

    private boolean isLikelyEmptyCatch(String[] patchLines, int catchLineIndex) {
        String catchLine = patchLines[catchLineIndex].replaceFirst("^\\+", "").trim();
        if (catchLine.contains("{}")) {
            return true;
        }

        for (int index = catchLineIndex + 1; index < patchLines.length; index++) {
            String line = patchLines[index];
            if (line.startsWith("@@")) {
                return false;
            }
            if (!line.startsWith("+") && !line.startsWith(" ")) {
                continue;
            }
            String code = line.substring(1).trim();
            if (code.isBlank()) {
                continue;
            }
            return code.equals("}");
        }
        return false;
    }
}
