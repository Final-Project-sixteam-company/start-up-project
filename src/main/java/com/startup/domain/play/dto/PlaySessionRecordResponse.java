package com.startup.domain.play.dto;

import java.time.LocalDateTime;

public record PlaySessionRecordResponse(
        String recordId,
        Long sessionId,
        Long scenarioId,
        String scenarioTitle,
        String status,
        Integer score,
        String grade,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
}
