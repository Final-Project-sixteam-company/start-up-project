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
        Integer interrogationCount
) {
}
