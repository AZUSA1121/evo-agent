package com.example.evoagent.review;

public record Finding(
        String file,
        Integer line,
        String type,
        String level,
        String title,
        String evidence,
        String suggestion
) {
}
