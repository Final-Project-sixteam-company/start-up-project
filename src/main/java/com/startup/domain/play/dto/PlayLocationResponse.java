package com.startup.domain.play.dto;

import com.startup.domain.scenario.entity.ScenarioLocation;
import java.util.List;

public record PlayLocationResponse(
        Long locationId,
        String name,
        String description,
        Integer mapX,
        Integer mapY,
        int evidenceCount,
        List<EvidenceSummary> evidences
) {
    public record EvidenceSummary(Long evidenceId, String title) {}

    public static PlayLocationResponse from(ScenarioLocation location, List<EvidenceSummary> evidences) {
        return new PlayLocationResponse(
                location.getId(),
                location.getName(),
                location.getDescription(),
                location.getMapX(),
                location.getMapY(),
                evidences.size(),
                evidences
        );
    }
}
