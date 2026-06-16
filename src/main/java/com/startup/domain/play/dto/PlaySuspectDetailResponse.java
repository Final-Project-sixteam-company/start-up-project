package com.startup.domain.play.dto;

import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.Suspect;

import java.time.LocalDateTime;
import java.util.List;

public record PlaySuspectDetailResponse(
        Long suspectId,
        String name,
        String role,
        String relationToVictim,
        String publicProfile,
        String publicStatement,
        String alibi,
        String portraitImageUrl,
        List<RelatedEvidenceDto> relatedEvidences,
        List<InterrogationLogDto> interrogationLogs
) {
    public record RelatedEvidenceDto(
            Long evidenceId,
            String title,
            Boolean isUnlocked
    ) {
        public static RelatedEvidenceDto from(Evidence evidence) {
            return new RelatedEvidenceDto(evidence.getId(), evidence.getTitle(), true);
        }
    }

    public record InterrogationLogDto(
            Long interrogationId,
            QuestionType questionType,
            String question,
            String answer,
            Long presentedEvidenceId,
            LocalDateTime createdAt
    ) {
        public static InterrogationLogDto from(InterrogationLog log) {
            return new InterrogationLogDto(
                    log.getId(),
                    log.getQuestionType(),
                    log.getQuestion(),
                    log.getAnswer(),
                    log.getPresentedEvidenceId(),
                    log.getCreatedAt()
            );
        }
    }

    public static PlaySuspectDetailResponse of(
            Suspect suspect,
            String portraitImageUrl,
            List<Evidence> relatedUnlockedEvidences,
            List<InterrogationLog> interrogationLogs
    ) {
        return new PlaySuspectDetailResponse(
                suspect.getId(),
                suspect.getName(),
                suspect.getRole(),
                suspect.getRelationToVictim(),
                suspect.getPublicProfile(),
                suspect.getPublicStatement(),
                suspect.getAlibi(),
                portraitImageUrl,
                relatedUnlockedEvidences.stream()
                        .map(RelatedEvidenceDto::from)
                        .toList(),
                interrogationLogs.stream()
                        .map(InterrogationLogDto::from)
                        .toList()
        );
    }
}
