package com.startup.domain.ai.dto;

/**
 * 심문 완료 후 발행되는 이벤트.
 * domain/play에서 EventListener로 수신하여 증거 해금 조건을 체크한다.
 */
public record InterrogationCompletedEvent(
        Long sessionId,
        Long suspectId,
        Long presentedEvidenceId,
        int interrogationCount
) {
}
