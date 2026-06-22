package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;

import java.util.List;

public record ScenarioDetailResponse(
        Long scenarioId,
        String title,
        String description,
        String synopsis,
        String coverImageUrl,
        String mapImageUrl,
        ScenarioType scenarioType,
        ScenarioVisibility visibility,
        Difficulty difficulty,
        Integer estimatedPlayTimeMinutes,
        Integer playerCountMin,
        Integer playerCountMax,
        Integer playCount,
        Double averageRating,
        Integer ratingCount,
        Integer suspectCount,
        Integer evidenceCount,
        Integer hintCount,
        List<String> tags,
        CreatorDto creator,
        Boolean isBookmarked,
        Boolean canPlay
) {
    public record CreatorDto(
            Long creatorId,
            String nickname
    ) {}

    public static ScenarioDetailResponse from(
            Scenario scenario, String creatorNickname,int suspectCount, int evidenceCount, int hintCount,
            Boolean isBookmarked, Boolean canPlay) {
        return from(scenario, creatorNickname, suspectCount, evidenceCount, hintCount,
                isBookmarked, canPlay, null, null);
    }

    public static ScenarioDetailResponse from(
            Scenario scenario, String creatorNickname, int suspectCount, int evidenceCount, int hintCount,
            Boolean isBookmarked, Boolean canPlay, String coverImageUrl, String mapImageUrl) {
        return new ScenarioDetailResponse(
                scenario.getId(),
                scenario.getTitle(),
                scenario.getDescription(),
                scenario.getSynopsis(),
                coverImageUrl,
                mapImageUrl,
                scenario.getScenarioType(),
                scenario.getVisibility(),
                scenario.getDifficulty(),
                scenario.getEstimatedPlayTimeMinutes(),
                scenario.getPlayerCountMin(),
                scenario.getPlayerCountMax(),
                scenario.getPlayCount(),
                scenario.getAverageRating(),
                scenario.getRatingCount(),
                suspectCount,
                evidenceCount,
                hintCount,
                java.util.Collections.emptyList(), // TODO: 나중에 태그 목록 반환 로직 추가하기
                new CreatorDto(scenario.getCreatorId(), creatorNickname),
                isBookmarked,
                canPlay
        );
    }
}
