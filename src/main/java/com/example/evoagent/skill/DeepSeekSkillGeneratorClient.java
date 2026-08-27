package com.example.evoagent.skill;

import com.example.evoagent.agent.DeepSeekChatRequest;
import com.example.evoagent.agent.DeepSeekChatResponse;
import com.example.evoagent.agent.DeepSeekMessage;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Component
public class DeepSeekSkillGeneratorClient {

    private static final String DEFAULT_MODEL = "deepseek-v4-flash";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public DeepSeekSkillGeneratorClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public GeneratedSkillDraft generateSkill(FailureAnalysisItem failure) {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing DEEPSEEK_API_KEY environment variable");
        }

        String model = System.getenv("DEEPSEEK_MODEL");
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }

        DeepSeekChatRequest body = new DeepSeekChatRequest(
                model,
                List.of(
                        new DeepSeekMessage(
                                "system",
                                "You are a senior Java code review skill designer. Return only valid json."
                        ),
                        new DeepSeekMessage("user", buildPrompt(failure))
                ),
                false,
                2048,
                Map.of("type", "json_object"),
                Map.of("type", "disabled")
        );

        try {
            String requestJson = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("DeepSeek skill generation failed. status=%s body=%s"
                        .formatted(response.statusCode(), response.body()));
            }

            DeepSeekChatResponse chatResponse = objectMapper.readValue(response.body(), DeepSeekChatResponse.class);
            if (chatResponse.choices() == null || chatResponse.choices().isEmpty()) {
                throw new IllegalStateException("DeepSeek skill generation returned no choices");
            }
            String content = chatResponse.choices().get(0).message().content();
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("DeepSeek skill generation returned empty content");
            }
            return objectMapper.readValue(content, GeneratedSkillDraft.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("DeepSeek returned invalid skill JSON", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call or parse DeepSeek skill generation response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DeepSeek skill generation interrupted", e);
        }
    }

    private String buildPrompt(FailureAnalysisItem failure) {
        return """
                请根据下面的 Evaluation Failure Analysis 生成一个可注入 Code Review Agent prompt 的审查 Skill。

                这个 Skill 面向 Java/Spring 后端代码审查，目标是提升 Agent 对该失败模式的识别能力。
                内容必须具体、可执行、适合放入 LLM prompt，不要写空泛原则。

                返回内容必须是合法 JSON，不要输出 Markdown 代码块，不要输出 JSON 之外的文字。
                JSON 结构必须严格如下：
                {
                  "name": "简短英文 Skill 名称",
                  "category": "SECURITY|RELIABILITY|PERFORMANCE|CODE_QUALITY|EVALUATION|GENERAL",
                  "description": "中文一句话说明这个 skill 解决什么失败模式",
                  "content": "Markdown 格式的 skill 正文，包含 When to apply、Detection rules、Evidence to look for、Finding guidance、Suggested fix 五个部分"
                }

                Failure Analysis:
                - failureType: %s
                - caseId: %s
                - caseTitle: %s
                - file: %s
                - category: %s
                - level: %s
                - findingTitle: %s
                - summary: %s
                - recommendedSkillGoal: %s
                - evidenceKeywords: %s
                """.formatted(
                failure.failureType(),
                nullToText(failure.caseId()),
                nullToText(failure.caseTitle()),
                nullToText(failure.file()),
                nullToText(failure.category()),
                nullToText(failure.level()),
                nullToText(failure.findingTitle()),
                nullToText(failure.summary()),
                nullToText(failure.recommendedSkillGoal()),
                failure.evidenceKeywords() == null ? List.of() : failure.evidenceKeywords()
        );
    }

    private String nullToText(String value) {
        return value == null ? "N/A" : value;
    }
}
