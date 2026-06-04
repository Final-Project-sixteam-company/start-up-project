package com.startup.domain.play.dto;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.enums.PlaySessionStatus;

import java.time.LocalDateTime;

public record ActivePlaySessionResponse(
        boolean hasActiveSession,
        Long activeSessionId,
        Long scenarioId,
        PlaySessionStatus status,
        LocalDateTime startedAt
) {
    public static ActivePlaySessionResponse exists(PlaySession session) {
        return new ActivePlaySessionResponse(
                true,
                session.getId(),
                session.getScenarioId(),
                session.getStatus(),
                session.getStartedAt()
        );
    }

    public static ActivePlaySessionResponse none(Long scenarioId) {
        return new ActivePlaySessionResponse(
                false,
                null,
                scenarioId,
                null,
                null
        );
    }
}
