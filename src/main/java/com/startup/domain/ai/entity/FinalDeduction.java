package com.startup.domain.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "final_deductions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FinalDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "play_session_id", nullable = false, unique = true)
    private Long playSessionId;

    @Column(name = "selected_culprit_id")
    private Long selectedCulpritId;

    @Column(name = "motive_text", columnDefinition = "TEXT")
    private String motiveText;

    @Column(name = "method_text", columnDefinition = "TEXT")
    private String methodText;

    @Column(name = "cover_up_text", columnDefinition = "TEXT")
    private String coverUpText;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "grade", length = 20)
    private String grade;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "matched_parts", columnDefinition = "TEXT")
    private String matchedParts;

    @Column(name = "missed_parts", columnDefinition = "TEXT")
    private String missedParts;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private FinalDeduction(Long playSessionId, Long selectedCulpritId,
                           String motiveText, String methodText, String coverUpText,
                           Integer score, String grade, String feedback,
                           String matchedParts, String missedParts,
                           LocalDateTime submittedAt) {
        this.playSessionId = playSessionId;
        this.selectedCulpritId = selectedCulpritId;
        this.motiveText = motiveText;
        this.methodText = methodText;
        this.coverUpText = coverUpText;
        this.score = score;
        this.grade = grade;
        this.feedback = feedback;
        this.matchedParts = matchedParts;
        this.missedParts = missedParts;
        this.submittedAt = submittedAt;
    }
}
