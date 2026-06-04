package com.startup.domain.play.dto;

import java.time.LocalDateTime;

public record EvidenceUnlockResponse(
        Long evidenceId,
        Boolean isUnlocked,
        LocalDateTime unlockedAt
) {
}
