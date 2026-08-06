package com.example.evoagent.review;

import com.example.evoagent.report.ReviewMarkdownGenerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMarkdownGenerator markdownGenerator;

    public ReviewController(ReviewService reviewService, ReviewMarkdownGenerator markdownGenerator) {
        this.reviewService = reviewService;
        this.markdownGenerator = markdownGenerator;
    }

    @GetMapping("/github-pr")
    public ReviewReport reviewGitHubPullRequest(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam int prNumber
    ) {
        return reviewService.reviewGitHubPullRequest(owner, repo, prNumber);
    }

    @GetMapping(value = "/github-pr/markdown", produces = MediaType.TEXT_MARKDOWN_VALUE)
    public String reviewGitHubPullRequestAsMarkdown(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam int prNumber
    ) {
        ReviewReport report = reviewService.reviewGitHubPullRequest(owner, repo, prNumber);
        return markdownGenerator.generate(report);
    }
}
