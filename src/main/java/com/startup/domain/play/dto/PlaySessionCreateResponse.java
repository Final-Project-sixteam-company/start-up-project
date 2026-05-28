package com.startup.domain.play.dto;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.enums.PlaySessionStatus;

import java.time.LocalDateTime;

// 게임 세션 시작 응답 DTO (API 스펙 섹션 9.1)
public record PlaySessionCreateResponse(
        Long sessionId,
        Long scenarioId,
        PlaySessionStatus status,
        LocalDateTime startedAt
) {
    public static PlaySessionCreateResponse from(PlaySession session) {
        return new PlaySessionCreateResponse(
                session.getId(),
                session.getScenarioId(),
                session.getStatus(),
                session.getStartedAt()
        );
    }
}
