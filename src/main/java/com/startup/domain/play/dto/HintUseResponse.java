package com.startup.domain.play.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record HintUseResponse(
        Long hintId,
        @Schema(description = "해금된 힌트의 실제 내용")
        String content,
        @Schema(description = "힌트 사용으로 인한 패널티 점수")
        Integer penaltyScore,

        @Schema(description = "힌트 사용 시각")
        LocalDateTime usedAt
) {
}
