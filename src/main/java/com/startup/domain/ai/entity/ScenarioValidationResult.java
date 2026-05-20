package com.startup.domain.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "scenario_validation_results",
        indexes = {
                @Index(name = "idx_scenario_validation_results_scenario", columnList = "scenario_id, checked_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioValidationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "validation_status", nullable = false, length = 30)
    private String validationStatus;

    @Column(name = "validation_score")
    private Integer validationScore;

    @Column(name = "problem_summary", columnDefinition = "TEXT")
    private String problemSummary;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "check_items_json", columnDefinition = "JSON")
    private String checkItemsJson;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ScenarioValidationResult(Long scenarioId,
                                     String validationStatus,
                                     Integer validationScore,
                                     String problemSummary,
                                     String suggestion,
                                     String checkItemsJson,
                                     LocalDateTime checkedAt) {
        this.scenarioId = scenarioId;
        this.validationStatus = validationStatus;
        this.validationScore = validationScore;
        this.problemSummary = problemSummary;
        this.suggestion = suggestion;
        this.checkItemsJson = checkItemsJson;
        this.checkedAt = checkedAt;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.checkedAt == null) {
            this.checkedAt = now;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
    }
}
