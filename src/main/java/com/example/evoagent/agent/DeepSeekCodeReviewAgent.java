package com.example.evoagent.agent;

import com.example.evoagent.github.PullRequestFile;
import com.example.evoagent.rag.ReviewContext;
import com.example.evoagent.skill.AgentSkill;
import com.example.evoagent.skill.SkillPromptService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeepSeekCodeReviewAgent {

    private final DeepSeekClient deepSeekClient;
    private final SkillPromptService skillPromptService;

    public DeepSeekCodeReviewAgent(
            DeepSeekClient deepSeekClient,
            SkillPromptService skillPromptService
    ) {
        this.deepSeekClient = deepSeekClient;
        this.skillPromptService = skillPromptService;
    }

    public DeepSeekReviewResponse review(List<PullRequestFile> files, ReviewContext reviewContext) {
        String prompt = buildPrompt(files, reviewContext);
        DeepSeekReviewResponse response = deepSeekClient.reviewCode(prompt);
        return normalize(response);
    }

    private DeepSeekReviewResponse normalize(DeepSeekReviewResponse response) {
        if (response == null) {
            return emptyReview();
        }

        return new DeepSeekReviewResponse(
                isBlank(response.summary()) ? "未发现明显问题，建议按常规流程完成构建与回归测试。" : response.summary(),
                isBlank(response.riskLevel()) ? "LOW" : response.riskLevel(),
                response.keyChanges() == null ? List.of() : response.keyChanges(),
                response.testSuggestions() == null ? List.of() : response.testSuggestions(),
                response.findings() == null ? List.of() : response.findings()
        );
    }

    private DeepSeekReviewResponse emptyReview() {
        return new DeepSeekReviewResponse(
                "未发现明显问题，建议按常规流程完成构建与回归测试。",
                "LOW",
                List.of(),
                List.of("运行项目测试套件，确认 PR 修改没有引入回归。"),
                List.of()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String buildPrompt(List<PullRequestFile> files, ReviewContext reviewContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                你是一个面向企业 Java 后端项目的资深 Code Review Agent。

                请用中文审查下面的 GitHub Pull Request patch，并结合检索到的完整源码上下文。

                你的目标不是简单挑刺，而是给研发团队一份可以直接放到 PR 评论区的审查结论。
                请重点关注：
                1. 安全风险：SQL 注入、权限绕过、硬编码密钥、敏感信息泄露、Webhook 签名校验缺失。
                2. 可靠性风险：空指针、异常吞掉、事务失效、幂等性问题、Redis/MQ/外部 API 一致性问题。
                3. 性能风险：N+1 查询、慢 SQL、循环调用数据库或远程 API、大对象/大响应体问题。
                4. 工程质量：边界条件、错误提示、日志可观测性、可测试性、代码职责划分。
                5. 本项目特定场景：GitHub API 调用、DeepSeek API 调用、PR Webhook 异步执行、Code RAG 上下文使用、Markdown Review 报告生成。

                返回内容必须是合法 JSON，不要输出 Markdown，不要输出 JSON 之外的文字。
                所有字段值都必须是 JSON 字符串、数字、数组或对象；字符串中的英文双引号必须转义为 \\"，换行必须转义为 \\n。
                evidence 和 suggestion 里不要粘贴大段源码；如果必须引用代码，只引用方法名、类名、字段名或一行以内的短片段。
                JSON 结构必须严格如下：
                {
                  "summary": "中文总体评价，2到4句话。说明本次 PR 做了什么、整体风险如何、是否建议合并。",
                  "riskLevel": "HIGH|MEDIUM|LOW",
                  "keyChanges": [
                    "中文概括一个关键变更点"
                  ],
                  "testSuggestions": [
                    "中文说明一个建议补充或执行的测试"
                  ],
                  "findings": [
                    {
                      "file": "path/to/File.java",
                      "line": 12,
                      "type": "SECURITY|RELIABILITY|PERFORMANCE|CODE_QUALITY",
                      "level": "HIGH|MEDIUM|LOW",
                      "title": "中文问题标题",
                      "evidence": "中文说明风险证据，必须结合具体代码或 patch",
                      "suggestion": "中文说明如何修复或改进"
                    }
                  ]
                }

                如果没有发现明确问题，也必须给出有信息量的中文 summary、keyChanges 和 testSuggestions，findings 返回空数组。
                不要编造不存在的 bug。只有证据明确时才放入 findings。

                当前激活的审查 Skills：
                """);

        appendActiveSkills(prompt);

        prompt.append("""

                检索到的源码上下文：
                """);

        if (reviewContext != null && reviewContext.codeContexts() != null) {
            for (var context : reviewContext.codeContexts()) {
                prompt.append("\n\n--- CONTEXT FILE: ").append(context.path()).append(" ---\n");
                prompt.append(context.content());
            }
        }

        prompt.append("""

                Pull request patch：
                """);

        for (PullRequestFile file : files) {
            prompt.append("\n\n--- FILE: ").append(file.filename()).append(" ---\n");
            prompt.append("status: ").append(file.status()).append("\n");
            prompt.append("additions: ").append(file.additions()).append("\n");
            prompt.append("deletions: ").append(file.deletions()).append("\n");
            prompt.append(file.patch() == null ? "" : file.patch());
        }

        return prompt.toString();
    }

    private void appendActiveSkills(StringBuilder prompt) {
        List<AgentSkill> activeSkills = skillPromptService.currentPromptSkills();
        if (activeSkills.isEmpty()) {
            prompt.append("\n当前没有激活 Skills。\n");
            return;
        }

        for (AgentSkill skill : activeSkills) {
            prompt.append("\n\n--- SKILL: ")
                    .append(skill.name())
                    .append(" v")
                    .append(skill.version())
                    .append(" ---\n");
            prompt.append("Category: ").append(nullToText(skill.category())).append("\n");
            prompt.append("Description: ").append(nullToText(skill.description())).append("\n");
            prompt.append("Source: ").append(skill.source()).append("\n");
            if (skill.sourceCaseId() != null && !skill.sourceCaseId().isBlank()) {
                prompt.append("Source Case: ").append(skill.sourceCaseId()).append("\n");
            }
            prompt.append("\n");
            prompt.append(nullToText(skill.content())).append("\n");
        }
    }

    private String nullToText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
