package com.startup.domain.play.dto;

import com.startup.domain.scenario.enums.EvidenceImportance;

import java.util.List;

// 플레이 세션 증거 목록 조회 응답 DTO (API 스펙 섹션 9.4)
public record PlayEvidenceResponse(
        Long evidenceId,
        String title,
        String oneLine,
        String description,
        String imageAssetKey,
        String locationName,
        EvidenceImportance importance,
        Boolean isUnlocked,
        String unlockHint,
        String imageUrl,
        List<RelatedSuspectDto> relatedSuspects
) {
    // 증거와 관련된 용의자 간략 정보
    public record RelatedSuspectDto(
            Long suspectId,
            String name
    ) {
    }
}
