package com.startup.domain.ai.dto;

import java.util.List;

public record AiValidationOutcome(
        List<ValidationCheckItem> checkItems,
        String problemSummary,
        String suggestion,
        boolean aiFailed
) {
    public static AiValidationOutcome failed() {
        return new AiValidationOutcome(
                List.of(),
                "AI 검증을 완료하지 못했습니다. 재시도해 주세요.",
                "AI 검증 실패로 공개 가능 여부를 판단할 수 없습니다.",
                true
        );
    }
}
