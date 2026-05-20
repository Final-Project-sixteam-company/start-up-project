package com.startup.domain.ai.dto;

import java.util.List;

public record ScenarioValidationResponse(
        Long scenarioId,
        String validationStatus,
        Integer validationScore,
        String problemSummary,
        String suggestion,
        List<CheckItemDto> checkItems
) {
    public record CheckItemDto(
            String name,
            boolean passed
    ) {
    }
}
