package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioSearchCondition {
    private String keyword;
    private ScenarioType type;
    private Difficulty difficulty;
    private ScenarioVisibility visibility;
    private Integer minPlayers;
    private Integer maxPlayers;
    private Integer maxPlayTime;
    private String sort;
}
