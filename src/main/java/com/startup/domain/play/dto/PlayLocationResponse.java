package com.startup.domain.play.dto;

import com.startup.domain.scenario.entity.ScenarioLocation;

public record PlayLocationResponse(
        Long locationId,
        String name,
        String description,
        Integer mapX,
        Integer mapY,
        int evidenceCount
) {
    public static PlayLocationResponse from(ScenarioLocation location, int evidenceCount) {
        return new PlayLocationResponse(
                location.getId(),
                location.getName(),
                location.getDescription(),
                location.getMapX(),
                location.getMapY(),
                evidenceCount
        );
    }
}
