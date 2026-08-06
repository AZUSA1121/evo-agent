package com.example.evoagent.github;

public record GitHubContentResponse(
        String path,
        String content,
        String encoding
) {
}
