package com.example.evoagent.runtime.node;

import com.example.evoagent.github.GitHubClient;
import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.runtime.AgentTask;
import com.example.evoagent.runtime.RuntimeContext;
import com.example.evoagent.runtime.RuntimeNode;
import com.example.evoagent.runtime.RuntimeNodeName;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FetchPrDiffNode implements RuntimeNode {

    private final GitHubClient gitHubClient;

    public FetchPrDiffNode(GitHubClient gitHubClient) {
        this.gitHubClient = gitHubClient;
    }

    @Override
    public RuntimeNodeName name() {
        return RuntimeNodeName.FETCH_PR_DIFF;
    }

    @Override
    public String execute(AgentTask task, RuntimeContext context) {
        List<PullRequestFile> files = gitHubClient.getPullRequestFiles(task.owner(), task.repo(), task.prNumber());
        context.setPullRequestFiles(files);

        int additions = files.stream()
                .map(PullRequestFile::additions)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        int deletions = files.stream()
                .map(PullRequestFile::deletions)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        return "changedFiles=%d additions=%d deletions=%d".formatted(files.size(), additions, deletions);
    }
}
