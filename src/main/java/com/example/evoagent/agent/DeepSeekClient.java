package com.example.evoagent.agent;

import org.springframework.stereotype.Component;
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
            return objectMapper.readValue(content, DeepSeekReviewResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call or parse DeepSeek API response", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DeepSeek API request interrupted", e);
        }
    }
}
