package com.startup.domain.ai.dto;

import com.startup.domain.ai.enums.QuestionType;

import java.time.LocalDateTime;

public record InterrogationLogResponse(
        Long interrogationId,
        Long suspectId,
        String suspectName,
        QuestionType questionType,
        String question,
        String answer,
        PresentedEvidenceDto presentedEvidence,
        LocalDateTime createdAt
) {
    public record PresentedEvidenceDto(Long evidenceId, String title) {}
}
