package com.example.evoagent.github;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/github")
public class GitHubPrController {

    private final GitHubClient gitHubClient;

    public GitHubPrController(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    @GetMapping("/pr/files")
    public List<PullRequestFile> getPullRequestFiles(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam int prNumber
    ) {
        return gitHubClient.getPullRequestFiles(owner, repo, prNumber);
    }
}
