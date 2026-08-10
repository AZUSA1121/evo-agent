package com.example.evoagent.runtime.node;

import com.example.evoagent.rag.CodeRagService;
import com.example.evoagent.rag.ReviewContext;
import com.example.evoagent.runtime.AgentTask;
import com.example.evoagent.runtime.RuntimeContext;
import com.example.evoagent.runtime.RuntimeNode;
import com.example.evoagent.runtime.RuntimeNodeName;
import org.springframework.stereotype.Component;

@Component
public class RetrieveCodeContextNode implements RuntimeNode {

    private final CodeRagService codeRagService;

    public RetrieveCodeContextNode(CodeRagService codeRagService) {
        this.codeRagService = codeRagService;
    }

    @Override
    public RuntimeNodeName name() {
        return RuntimeNodeName.RETRIEVE_CODE_CONTEXT;
    }

    @Override
    public String inputSummary(AgentTask task, RuntimeContext context) {
        return "changedFiles=%d".formatted(context.getPullRequestFiles().size());
    }

    @Override
    public String execute(AgentTask task, RuntimeContext context) {
        ReviewContext reviewContext = codeRagService.retrieve(
                task.owner(),
                task.repo(),
                task.prNumber(),
                context.getPullRequestFiles()
        );
        context.setReviewContext(reviewContext);
        int contextFileCount = reviewContext.codeContexts() == null ? 0 : reviewContext.codeContexts().size();
        return "contextFiles=%d".formatted(contextFileCount);
    }
}
