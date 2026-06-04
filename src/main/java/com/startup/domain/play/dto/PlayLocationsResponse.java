package com.startup.domain.play.dto;

import java.util.List;

// 플레이 세션 현장/장소 정보 조회 응답 DTO
public record PlayLocationsResponse(
        Long sessionId,
        Long scenarioId,
        String scenarioTitle,
        String mapImageUrl,
        List<LocationDto> locations
) {
    public record LocationDto(
            Long locationId,
            String locationCode,
            String name,
            String floor,
            String description,
            String imageAssetKey,
            String imageUrl,
            Integer mapX,
            Integer mapY,
            Integer totalEvidenceCount,
            Integer unlockedEvidenceCount
    ) {
    }
}
