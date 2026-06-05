package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.ScenarioVisibility;

public record ScenarioPublishRequest(
        ScenarioVisibility visibility
) {}
