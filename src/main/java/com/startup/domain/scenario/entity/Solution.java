package com.startup.domain.scenario.entity;

import com.startup.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(
        name = "solutions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_solutions_scenario", columnNames = {"scenario_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Solution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    // 진범의 용의자 ID
    @Column(name = "culprit_suspect_id", nullable = false)
    private Long culpritSuspectId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String motive;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String method;

    @Column(name = "cover_up", columnDefinition = "TEXT")
    private String coverUp;

    @Column(name = "full_explanation", columnDefinition = "TEXT")
    private String fullExplanation;

    // 핵심 증거 ID 목록 (쉼표 구분 문자열, 예: "2,6,7,8")
    @Column(name = "key_evidence_ids", columnDefinition = "TEXT")
    private String keyEvidenceIds;

    @Builder
    public Solution(Long scenarioId, Long culpritSuspectId, String motive,
                    String method, String coverUp, String fullExplanation, String keyEvidenceIds) {
        this.scenarioId = scenarioId;
        this.culpritSuspectId = culpritSuspectId;
        this.motive = motive;
        this.method = method;
        this.coverUp = coverUp;
        this.fullExplanation = fullExplanation;
        this.keyEvidenceIds = keyEvidenceIds;
    }

    // "2,6,7,8" 형태의 문자열을 List<Long>으로 파싱
    public List<Long> parseKeyEvidenceIds() {
        if (keyEvidenceIds == null || keyEvidenceIds.isBlank()) return List.of();
        return Arrays.stream(keyEvidenceIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }
}
