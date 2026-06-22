package com.startup.domain.ai.dto;

import com.startup.domain.ai.enums.ValidationSeverity;
import com.startup.domain.ai.enums.ValidationSource;

public record ValidationCheckItem(
        String key,
        String name,
        boolean passed,
        int score,
        int maxScore,
        ValidationSource source,
        ValidationSeverity severity
) {
}
