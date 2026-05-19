package com.startup.domain.ai.dto;

import com.startup.domain.ai.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "심문 응답")
public record InterrogationResponse(
        @Schema(description = "용의자 ID")
        Long suspectId,

        @Schema(description = "용의자 이름")
        String suspectName,

        @Schema(description = "AI 답변")
        String answer,

        @Schema(description = "질문 유형")
        QuestionType questionType,

        @Schema(description = "새로 해금된 증거 ID 목록")
        List<Long> newlyUnlockedEvidenceIds
) {
}
