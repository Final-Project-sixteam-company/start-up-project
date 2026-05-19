package com.startup.domain.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "final_deduction_evidences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"final_deduction_id", "evidence_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FinalDeductionEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "final_deduction_id", nullable = false)
    private Long finalDeductionId;

    @Column(name = "evidence_id", nullable = false)
    private Long evidenceId;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    public FinalDeductionEvidence(Long finalDeductionId, Long evidenceId) {
        this.finalDeductionId = finalDeductionId;
        this.evidenceId = evidenceId;
    }
}
