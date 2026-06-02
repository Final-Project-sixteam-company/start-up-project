package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioType;

public record ScenarioSummaryResponse(
        Long scenarioId,
        String title,
        String description,
        String thumbnailUrl,
        ScenarioType scenarioType,
        Difficulty difficulty,
        Integer estimatedPlayTimeMinutes,
        Integer playerCountMin,
        Integer playerCountMax,
        Integer suspectCount,
        Integer evidenceCount,
        Integer playCount,
        Double averageRating,
        Boolean isBookmarked
) {
    public static ScenarioSummaryResponse from(Scenario scenario, int suspectCount, int evidenceCount, Boolean isBookmarked) {
        return new ScenarioSummaryResponse(
                scenario.getId(),
                scenario.getTitle(),
                scenario.getDescription(),
                null, // TODO: 나중에 썸네일 이미지 경로 반환 로직 추가하기
                scenario.getScenarioType(),
                scenario.getDifficulty(),
                scenario.getEstimatedPlayTimeMinutes(),
                scenario.getPlayerCountMin(),
                scenario.getPlayerCountMax(),
                suspectCount,
                evidenceCount,
                scenario.getPlayCount(),
                scenario.getAverageRating(),
                isBookmarked
        );
    }
}
