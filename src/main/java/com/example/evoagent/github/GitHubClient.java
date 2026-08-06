package com.example.evoagent.github;

import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    public PullRequestInfo getPullRequestInfo(String owner, String repo, int pullNumber) {
        String url = "https://api.github.com/repos/%s/%s/pulls/%d"
                .formatted(owner, repo, pullNumber);
        return sendGitHubGet(url, PullRequestInfo.class);
    }

    public String getFileContent(String owner, String repo, String path, String ref) {
        String url = "https://api.github.com/repos/%s/%s/contents/%s"
                .formatted(owner, repo, path);
        if (ref != null && !ref.isBlank()) {
            url = url + "?ref=" + ref;
        }

        GitHubContentResponse response = sendGitHubGet(url, GitHubContentResponse.class);
        if (response.content() == null || response.content().isBlank()) {
            return "";
        }
        if (!"base64".equalsIgnoreCase(response.encoding())) {
            return response.content();
        }

        String normalizedContent = response.content().replace("\n", "");
        byte[] decoded = Base64.getDecoder().decode(normalizedContent);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public void createPullRequestComment(String owner, String repo, int pullNumber, String markdown) {
        String url = "https://api.github.com/repos/%s/%s/issues/%d/comments"
                .formatted(owner, repo, pullNumber);
        GitHubCommentRequest body = new GitHubCommentRequest(markdown);
        sendGitHubPost(url, body);
    }

    private <T> T sendGitHubGet(String url, Class<T> responseType) {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing GITHUB_TOKEN environment variable");
        }

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
            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse GitHub API response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub API request interrupted", e);
        }
    }

    private void sendGitHubPost(String url, Object body) {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing GITHUB_TOKEN environment variable");
        }

        try {
            String requestJson = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("Content-Type", "application/json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("GitHub API request failed. status=%s body=%s"
                        .formatted(response.statusCode(), response.body()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to send GitHub API request", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GitHub API request interrupted", e);
        }
    }
}
