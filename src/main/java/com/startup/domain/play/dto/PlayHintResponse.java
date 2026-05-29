package com.startup.domain.play.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 힌트 목록 조회 응답 DTO
 * @param content : 힌트를 실제 사용(/hints/{hintId}/use) 후에만 반환
 * @param isAvailable : false이면 content를 null로 내려주어 힌트 내용을 숨긴다. true이더라도 use API 호출 전까지 content는 null
 * @param isUsed : true이면 이미 사용한 힌트 — content는 힌트 사용 API(/hints/{hintId}/use)로만 노출
 */
public record PlayHintResponse(
        Long hintId,
        Integer hintLevel,

        @Schema(description = "힌트 내용 (사용 완료 시에만 노출)")
        String content,

        @Schema(description = "현재 경과 시간 기준으로 해금 가능 여부")
        Boolean isAvailable,

        @Schema(description = "이 세션에서 실제로 사용(열람)했는지 여부")
        Boolean isUsed,

        @Schema(description = "미해금 상태일 때 잠금 해제까지 남은 시간(분). 해금 가능 상태변 null")
        @JsonProperty("unlockAfterMinutes")
        Integer remainingMinutes,

        Integer penaltyScore
) {
}
