package com.example.evoagent.runtime.node;

import com.example.evoagent.agent.DeepSeekCodeReviewAgent;
import com.example.evoagent.agent.DeepSeekReviewResponse;
import com.example.evoagent.runtime.AgentTask;
import com.example.evoagent.runtime.RuntimeContext;
import com.example.evoagent.runtime.RuntimeNode;
import com.example.evoagent.runtime.RuntimeNodeName;
import org.springframework.stereotype.Component;

@Component
public class RunDeepSeekReviewNode implements RuntimeNode {

    private final DeepSeekCodeReviewAgent codeReviewAgent;

    public RunDeepSeekReviewNode(DeepSeekCodeReviewAgent codeReviewAgent) {
        this.codeReviewAgent = codeReviewAgent;
    }

    @Override
    public RuntimeNodeName name() {
        return RuntimeNodeName.RUN_DEEPSEEK_REVIEW;
    }

    @Override
    public String inputSummary(AgentTask task, RuntimeContext context) {
        int contextFileCount = context.getReviewContext() == null
                || context.getReviewContext().codeContexts() == null
                ? 0
                : context.getReviewContext().codeContexts().size();
        return "changedFiles=%d contextFiles=%d".formatted(context.getPullRequestFiles().size(), contextFileCount);
    }

    @Override
    public String execute(AgentTask task, RuntimeContext context) {
        DeepSeekReviewResponse aiReview = codeReviewAgent.review(
                context.getPullRequestFiles(),
                context.getReviewContext()
        );
        context.setAiReview(aiReview);
        int findingCount = aiReview.findings() == null ? 0 : aiReview.findings().size();
        return "riskLevel=%s findings=%d".formatted(aiReview.riskLevel(), findingCount);
    }
}
