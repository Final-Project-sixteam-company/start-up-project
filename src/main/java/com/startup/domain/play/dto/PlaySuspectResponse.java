package com.startup.domain.play.dto;

// 플레이 세션 용의자 목록 조회 응답 DTO (API 스펙 섹션 9.7)
public record PlaySuspectResponse(
        Long suspectId,
        String name,
        String role,
        String relationToVictim,
        String publicStatement,
        String alibi,
        String portraitImageUrl,
        Integer suspicionLevel,
        Integer interrogationCount,
        // 최종 범인 후보로 지목 가능한지 여부. FE는 이 값이 true인 용의자만 지목 후보로 노출한다.
        Boolean culpritEligible
) {
}
