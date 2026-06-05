package com.startup.domain.scenario.dto;

public record ScenarioUpdateResponse(
        Long scenarioId,
        boolean updated
) {}
