package com.startup.domain.play.dto;

// 힌트 목록 조회 응답 DTO
// isAvailable=false이면 content를 null로 내려주어 힌트 내용을 숨긴다.
// isUsed=true이면 이미 사용한 힌트 — content는 힌트 사용 API(/hints/{hintId}/use)로만 노출한다.
public record PlayHintResponse(
        Long hintId,
        Integer hintLevel,
        String content,           // isAvailable && isUsed 일 때만 값이 있고, 나머지는 null
        Boolean isAvailable,      // 현재 경과 시간 기준으로 해금 가능 여부
        Boolean isUsed,           // 이 세션에서 실제로 사용(열람)했는지 여부
        Integer unlockAfterMinutes, // 미해금 상태일 때 잠금 해제까지 남은 시간(분)
        Integer penaltyScore
) {
}
