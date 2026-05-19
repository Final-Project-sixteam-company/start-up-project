package com.startup.domain.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "suspect_response_policies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SuspectResponsePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suspect_id", nullable = false)
    private Long suspectId;

    @Column(name = "condition_key", nullable = false)
    private String conditionKey;

    @Column(name = "user_intent")
    private String userIntent;

    @Column(name = "required_evidence_ids", columnDefinition = "JSON")
    private String requiredEvidenceIds;

    @Column(name = "excluded_evidence_ids", columnDefinition = "JSON")
    private String excludedEvidenceIds;

    @Column(name = "presented_evidence_id")
    private Long presentedEvidenceId;

    @Column(name = "policy_text", nullable = false, columnDefinition = "TEXT")
    private String policyText;

    @Column(name = "allowed_facts", columnDefinition = "JSON")
    private String allowedFacts;

    @Column(name = "forbidden_facts", columnDefinition = "JSON")
    private String forbiddenFacts;

    @Column(name = "tone")
    private String tone;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
