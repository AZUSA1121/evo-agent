package com.example.evoagent.rag;

import com.example.evoagent.github.GitHubClient;
import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.github.PullRequestInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CodeRagService {

    private final GitHubClient gitHubClient;

    public CodeRagService(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    public ReviewContext retrieve(String owner, String repo, int prNumber, List<PullRequestFile> changedFiles) {
        PullRequestInfo pullRequestInfo = gitHubClient.getPullRequestInfo(owner, repo, prNumber);
        String ref = pullRequestInfo.head() == null ? null : pullRequestInfo.head().sha();

        List<CodeContext> contexts = changedFiles.stream()
                .filter(file -> file.filename() != null)
                .filter(file -> !"removed".equalsIgnoreCase(file.status()))
                .limit(8)
                .map(file -> loadContext(owner, repo, file.filename(), ref))
                .filter(Objects::nonNull)
                .toList();

        return new ReviewContext(contexts);
    }

    private CodeContext loadContext(String owner, String repo, String path, String ref) {
        String content = gitHubClient.getFileContent(owner, repo, path, ref);
        if (content == null || content.isBlank()) {
            return null;
        }
        return new CodeContext(path, trimLargeFile(content));
    }

    private String trimLargeFile(String content) {
        int maxChars = 12000;
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "\n\n// ... file truncated for review context ...";
    }
}
