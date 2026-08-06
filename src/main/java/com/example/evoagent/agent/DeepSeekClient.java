package com.example.evoagent.agent;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private static final String DEFAULT_MODEL = "deepseek-v4-flash";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public DeepSeekClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DeepSeekReviewResponse reviewCode(String prompt) {
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
                                "You are a strict Java code review agent. Return only valid json."
                        ),
                        new DeepSeekMessage("user", prompt)
                ),
                false,
                4096,
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
                throw new IllegalStateException("DeepSeek API request failed. status=%s body=%s"
                        .formatted(response.statusCode(), response.body()));
            }

            DeepSeekChatResponse chatResponse = objectMapper.readValue(response.body(), DeepSeekChatResponse.class);
            if (chatResponse.choices() == null || chatResponse.choices().isEmpty()) {
                throw new IllegalStateException("DeepSeek API returned no choices");
            }
            if (chatResponse.choices().get(0).message() == null
                    || chatResponse.choices().get(0).message().content() == null
                    || chatResponse.choices().get(0).message().content().isBlank()) {
                throw new IllegalStateException("DeepSeek API returned empty review content");
            }

            String content = chatResponse.choices().get(0).message().content();
            try {
                return objectMapper.readValue(content, DeepSeekReviewResponse.class);
            } catch (JacksonException e) {
                log.warn("DeepSeek returned invalid JSON review content. content={}", abbreviate(content), e);
                return invalidJsonFallback();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call or parse DeepSeek API response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DeepSeek API request interrupted", e);
        }
    }

    private DeepSeekReviewResponse invalidJsonFallback() {
        return new DeepSeekReviewResponse(
                "DeepSeek 已返回审查内容，但返回格式不是合法 JSON，系统已降级生成本报告。建议重新触发一次 review；如果多次出现，可以继续收紧 prompt 或增加 JSON 修复步骤。",
                "MEDIUM",
                List.of("DeepSeek API 调用成功，但模型输出格式不稳定，导致结构化 findings 无法解析。"),
                List.of("重新 Redeliver 对应的 pull_request 事件，确认是否为偶发模型输出格式问题。"),
                List.of()
        );
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        int maxLength = 1000;
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
