package com.startup.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "심문 응답")
public record InterrogationResponse(
        @Schema(description = "심문 로그 ID", example = "500")
        Long interrogationId,

        @Schema(description = "용의자 ID")
        Long suspectId,

        @Schema(description = "용의자 이름")
        String suspectName,

        @Schema(description = "사용자 질문")
        String question,

        @Schema(description = "AI 답변")
        String answer,

        @Schema(description = "새로 해금된 증거 목록")
        List<UnlockedEvidenceDto> unlockedEvidences,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt
) {
    @Schema(description = "해금된 증거 요약")
    public record UnlockedEvidenceDto(
            Long id,
            String title
    ) {}
}
