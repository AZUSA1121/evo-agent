package com.example.evoagent.agent;

import java.util.List;

public record DeepSeekChatResponse(
        List<DeepSeekChoice> choices
) {
}
