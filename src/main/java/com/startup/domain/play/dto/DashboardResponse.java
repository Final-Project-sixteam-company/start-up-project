package com.startup.domain.play.dto;

import com.startup.domain.play.enums.PlaySessionStatus;

// 탐정 대시보드 조회 응답 DTO (API 스펙 섹션 9.2)
public record DashboardResponse(
        Long sessionId,
        Long scenarioId,
        String scenarioTitle,
        PlaySessionStatus status,
        Integer elapsedSeconds,
        Integer unlockedEvidenceCount,
        Integer totalEvidenceCount,
        Integer hintUsedCount,
        Integer interrogationCount,
        BriefingDto briefing
) {
    // 사건 브리핑 정보 (피해자, 발견 장소, 요약)
    public record BriefingDto(
            String victimName,
            String foundLocation,
            String summary
    ) {
    }
}
