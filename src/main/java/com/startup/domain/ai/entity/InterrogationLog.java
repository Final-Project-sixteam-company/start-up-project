package com.startup.domain.ai.entity;

import com.startup.domain.ai.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "interrogation_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class InterrogationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "play_session_id", nullable = false)
    private Long playSessionId;

    @Column(name = "suspect_id", nullable = false)
    private Long suspectId;

    @Column(name = "presented_evidence_id")
    private Long presentedEvidenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "ai_model")
    private String aiModel;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private InterrogationLog(Long playSessionId, Long suspectId, Long presentedEvidenceId,
                             QuestionType questionType, String question, String answer,
                             String aiModel) {
        this.playSessionId = playSessionId;
        this.suspectId = suspectId;
        this.presentedEvidenceId = presentedEvidenceId;
        this.questionType = questionType;
        this.question = question;
        this.answer = answer;
        this.aiModel = aiModel;
    }
}
