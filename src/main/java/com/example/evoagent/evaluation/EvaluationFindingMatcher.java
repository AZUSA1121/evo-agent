package com.example.evoagent.evaluation;

import com.example.evoagent.review.Finding;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class EvaluationFindingMatcher {

    public boolean matches(ExpectedFinding expected, Finding actual) {
        return fileMatches(expected.file(), actual.file())
                && textMatches(expected.type(), actual.type())
                && keywordMatches(expected.keywords(), actual);
    }

    private boolean fileMatches(String expectedFile, String actualFile) {
        if (isBlank(expectedFile) || isBlank(actualFile)) {
            return false;
        }

        String expected = normalizePath(expectedFile);
        String actual = normalizePath(actualFile);
        return expected.equals(actual)
                || expected.endsWith("/" + actual)
                || actual.endsWith("/" + expected);
    }

    private boolean textMatches(String expected, String actual) {
        if (isBlank(expected) || isBlank(actual)) {
            return false;
        }
        return expected.trim().equalsIgnoreCase(actual.trim());
    }

    private boolean keywordMatches(List<String> keywords, Finding actual) {
        if (keywords == null || keywords.isEmpty()) {
            return true;
        }

        String haystack = String.join(" ",
                nullToText(actual.title()),
                nullToText(actual.evidence()),
                nullToText(actual.suggestion()),
                nullToText(actual.type()),
                nullToText(actual.level())
        ).toLowerCase(Locale.ROOT);

        return keywords.stream()
                .filter(keyword -> !isBlank(keyword))
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(haystack::contains);
    }

    private String normalizePath(String path) {
        return path.replace('\\', '/').trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToText(String value) {
        return value == null ? "" : value;
    }
}
