package com.example.evoagent.github;

import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class GitHubClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public GitHubClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PullRequestFile> getPullRequestFiles(String owner, String repo, int pullNumber) {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing GITHUB_TOKEN environment variable");
        }

        String url = "https://api.github.com/repos/%s/%s/pulls/%d/files"
                .formatted(owner, repo, pullNumber);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("GitHub API request failed. status=%s body=%s"
                        .formatted(response.statusCode(), response.body()));
            }
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse GitHub API response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub API request interrupted", e);
        }
    }
}
