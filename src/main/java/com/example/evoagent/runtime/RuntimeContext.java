package com.example.evoagent.runtime;

import com.example.evoagent.agent.DeepSeekReviewResponse;
import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.rag.ReviewContext;
import com.example.evoagent.review.ReviewReport;

import java.util.List;

public class RuntimeContext {

    private List<PullRequestFile> pullRequestFiles = List.of();
    private ReviewContext reviewContext;
    private DeepSeekReviewResponse aiReview;
    private ReviewReport reviewReport;
    private String markdownReport;

    public List<PullRequestFile> getPullRequestFiles() {
        return pullRequestFiles;
    }

    public void setPullRequestFiles(List<PullRequestFile> pullRequestFiles) {
        this.pullRequestFiles = pullRequestFiles == null ? List.of() : pullRequestFiles;
    }

    public ReviewContext getReviewContext() {
        return reviewContext;
    }

    public void setReviewContext(ReviewContext reviewContext) {
        this.reviewContext = reviewContext;
    }

    public DeepSeekReviewResponse getAiReview() {
        return aiReview;
    }

    public void setAiReview(DeepSeekReviewResponse aiReview) {
        this.aiReview = aiReview;
    }

    public ReviewReport getReviewReport() {
        return reviewReport;
    }

    public void setReviewReport(ReviewReport reviewReport) {
        this.reviewReport = reviewReport;
    }

    public String getMarkdownReport() {
        return markdownReport;
    }

    public void setMarkdownReport(String markdownReport) {
        this.markdownReport = markdownReport;
    }

    public RuntimeContext copy() {
        RuntimeContext copy = new RuntimeContext();
        copy.setPullRequestFiles(pullRequestFiles);
        copy.setReviewContext(reviewContext);
        copy.setAiReview(aiReview);
        copy.setReviewReport(reviewReport);
        copy.setMarkdownReport(markdownReport);
        return copy;
    }
}
