package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.ScenarioStatus;

public record ScenarioDeleteResponse(
        Long scenarioId,
        ScenarioStatus status
) {
}
