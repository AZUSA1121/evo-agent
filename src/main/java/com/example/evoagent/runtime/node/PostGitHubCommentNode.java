package com.example.evoagent.runtime.node;

import com.example.evoagent.github.GitHubClient;
import com.example.evoagent.runtime.AgentTask;
import com.example.evoagent.runtime.RuntimeContext;
import com.example.evoagent.runtime.RuntimeNode;
import com.example.evoagent.runtime.RuntimeNodeName;
import org.springframework.stereotype.Component;

@Component
public class PostGitHubCommentNode implements RuntimeNode {

    private final GitHubClient gitHubClient;

    public PostGitHubCommentNode(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    @Override
    public RuntimeNodeName name() {
        return RuntimeNodeName.POST_GITHUB_COMMENT;
    }

    @Override
    public String inputSummary(AgentTask task, RuntimeContext context) {
        String markdown = context.getMarkdownReport();
        return "markdownLength=%d".formatted(markdown == null ? 0 : markdown.length());
    }

    @Override
    public String execute(AgentTask task, RuntimeContext context) {
        gitHubClient.createPullRequestComment(
                task.owner(),
                task.repo(),
                task.prNumber(),
                context.getMarkdownReport()
        );
        return "commentPosted=true";
    }
}
