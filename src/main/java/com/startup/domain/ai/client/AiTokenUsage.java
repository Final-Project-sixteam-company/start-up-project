package com.startup.domain.ai.client;

public record AiTokenUsage(
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
