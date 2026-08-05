package com.example.evoagent.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/github-pr")
    public ReviewReport reviewGitHubPullRequest(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam int prNumber
    ) {
        return reviewService.reviewGitHubPullRequest(owner, repo, prNumber);
    }
}
