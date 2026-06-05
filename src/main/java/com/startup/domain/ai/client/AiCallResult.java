package com.startup.domain.ai.client;

public record AiCallResult(
        String text,
        String modelName,
        long latencyMs,
        AiTokenUsage tokenUsage,
        boolean fallbackUsed
) {
}
