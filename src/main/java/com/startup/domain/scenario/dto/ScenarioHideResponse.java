package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.ScenarioStatus;

public record ScenarioHideResponse(
        Long scenarioId,
        ScenarioStatus status
) {
}
