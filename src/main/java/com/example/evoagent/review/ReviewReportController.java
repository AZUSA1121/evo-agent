package com.example.evoagent.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReviewReportController {

    private final ReviewReportRepository repository;

    public ReviewReportController(ReviewReportRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ReviewReportView> listReports(
            @RequestParam(required = false) String repo,
            @RequestParam(required = false) Integer prNumber
    ) {
        if (repo != null && !repo.isBlank() && prNumber != null) {
            return repository.findByRepositoryAndPullRequest(repo, prNumber);
        }
        return repository.findAll();
    }

    @GetMapping("/by-task/{taskId}")
    public ReviewReportView getLatestByTask(@PathVariable String taskId) {
        return repository.findLatestByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Review report not found for task: " + taskId));
    }
}
