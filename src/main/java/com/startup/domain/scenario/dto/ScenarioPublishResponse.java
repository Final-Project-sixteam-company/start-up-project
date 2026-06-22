package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioVisibility;

import java.time.LocalDateTime;

public record ScenarioPublishResponse(
        Long scenarioId,
        ScenarioVisibility visibility,
        ScenarioStatus status,
        LocalDateTime publishedAt
) {}
