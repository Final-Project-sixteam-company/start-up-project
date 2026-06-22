package com.startup.domain.ai.dto;

import java.util.List;

public record AiValidationResult(
        List<AiCheckItem> items,
        String problemSummary,
        String suggestion
) {
    public record AiCheckItem(
            String key,
            int score,
            String comment
    ) {
    }
}
