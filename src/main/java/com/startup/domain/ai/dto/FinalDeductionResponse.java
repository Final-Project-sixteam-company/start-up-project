package com.startup.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "최종 추리 제출 응답")
public record FinalDeductionResponse(
        @Schema(description = "최종 추리 ID")
        Long finalDeductionId,

        @Schema(description = "총점")
        Integer score,

        @Schema(description = "등급")
        String grade,

        @Schema(description = "피드백 요약")
        String feedbackSummary,

        @Schema(description = "결과 조회 가능 여부")
        Boolean resultAvailable,

        @Schema(description = "제출 시각")
        LocalDateTime submittedAt
) {}
