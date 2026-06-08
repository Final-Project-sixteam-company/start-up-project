package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.Difficulty;

public record ScenarioUpdateRequest(
        String title,
        String description,
        Difficulty difficulty,
        Integer estimatedPlayTimeMinutes
) {}
