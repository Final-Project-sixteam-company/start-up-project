package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.Difficulty;

public record ScenarioCreateRequest(
        String title,
        String description,
        String synopsis,
        Difficulty difficulty,
        Integer playerCountMin,
        Integer playerCountMax,
        Integer estimatedPlayTimeMinutes
) {}
