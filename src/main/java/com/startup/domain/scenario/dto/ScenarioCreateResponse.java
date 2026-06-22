package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.ScenarioStatus;

public record ScenarioCreateResponse(
        Long scenarioId,
        ScenarioStatus status
) {}
