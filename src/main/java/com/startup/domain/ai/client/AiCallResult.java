package com.startup.domain.ai.client;

import com.startup.domain.ai.dto.AiQuotaStatus;

public record AiCallResult(
        String text,
        String modelName,
        long latencyMs,
        AiTokenUsage tokenUsage,
        boolean fallbackUsed,
        AiQuotaStatus quotaStatus
) {
    public AiCallResult(String text,
                        String modelName,
                        long latencyMs,
                        AiTokenUsage tokenUsage,
                        boolean fallbackUsed) {
        this(text, modelName, latencyMs, tokenUsage, fallbackUsed, null);
    }
}
