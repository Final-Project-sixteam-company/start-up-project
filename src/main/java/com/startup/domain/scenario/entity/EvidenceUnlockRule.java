package com.startup.domain.scenario.entity;

import com.startup.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "evidence_unlock_rules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_evidence_unlock_rules_evidence", columnNames = {"evidence_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// YAML의 unlockRules 원본 조건을 보존한다. MVP는 phase 기반이고, 고도화 조건은 JSON으로 확장한다.
public class EvidenceUnlockRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "evidence_id", nullable = false)
    private Long evidenceId;

    @Column(name = "evidence_code", nullable = false, length = 100)
    private String evidenceCode;

    @Column(name = "unlock_type", nullable = false, length = 50)
    private String unlockType;

    @Column(name = "required_phase", length = 50)
    private String requiredPhase;

    @Column(name = "condition_json", columnDefinition = "TEXT")
    private String conditionJson;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    private EvidenceUnlockRule(Long scenarioId, Long evidenceId, String evidenceCode,
                               String unlockType, String requiredPhase, String conditionJson,
                               Integer sortOrder) {
        this.scenarioId = scenarioId;
        this.evidenceId = evidenceId;
        this.evidenceCode = evidenceCode;
        this.unlockType = unlockType;
        this.requiredPhase = requiredPhase;
        this.conditionJson = conditionJson;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }
}
