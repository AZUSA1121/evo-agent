package com.example.evoagent.agent;

import java.util.List;
import java.util.Map;

public record DeepSeekChatRequest(
        String model,
        List<DeepSeekMessage> messages,
        boolean stream,
        int max_tokens,
        Map<String, String> response_format,
        Map<String, String> thinking
) {
}
