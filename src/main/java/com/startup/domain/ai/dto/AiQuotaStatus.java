package com.startup.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI 호출 quota 상태")
public record AiQuotaStatus(
        @Schema(description = "quota scope", example = "SCENARIO_DAILY")
        String scope,

        @Schema(description = "현재 시나리오에서 오늘 사용한 실제 AI 호출 수", example = "51")
        long scenarioUsed,

        @Schema(description = "현재 시나리오 기준 일일 AI 호출 한도", example = "150")
        long scenarioLimit,

        @Schema(description = "현재 계정에서 오늘 사용한 실제 AI 호출 총량", example = "120")
        long accountUsed,

        @Schema(description = "현재 계정 기준 일일 AI 호출 총량 한도", example = "350")
        long accountLimit,

        @Schema(description = "현재 quota 안내 단계", example = "SUGGEST_EVIDENCE_REVIEW")
        String stage,

        @Schema(description = "프론트가 사용할 권장 action", example = "OPEN_EVIDENCE_TIMELINE")
        String recommendedAction,

        @Schema(description = "사용자에게 보여줄 수 있는 짧은 안내 메시지")
        String message,

        @Schema(description = "다음 안내 threshold. 더 이상 없으면 null", example = "70")
        Long nextThreshold,

        @Schema(description = "현재 시나리오 기준 남은 호출 수", example = "99")
        long remaining
) {
}
