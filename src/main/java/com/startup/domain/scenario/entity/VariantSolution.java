package com.startup.domain.scenario.entity;

import com.startup.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "variant_solutions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// ScenarioVariant 1 : 1 VariantSolution.
// 각 변주의 정답(범인, 동기, 방법, 알리바이)을 관리한다.
// 이 엔티티의 데이터는 절대 AI 프롬프트에 직접 전달하지 않는다.
public class VariantSolution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variant_id", nullable = false, unique = true)
    private Long variantId;

    @Column(name = "culprit_suspect_id", nullable = false)
    private Long culpritSuspectId;

    @Column(name = "culprit_code", length = 100)
    private String culpritCode;

    @Column(name = "culprit_name", nullable = false, length = 100)
    private String culpritName;

    @Column(name = "culprit_role", length = 100)
    private String culpritRole;

    @Column(columnDefinition = "TEXT")
    private String motive;

    @Column(columnDefinition = "TEXT")
    private String method;

    @Column(name = "cover_up", columnDefinition = "TEXT")
    private String coverUp;

    @Column(name = "full_explanation", columnDefinition = "TEXT")
    private String fullExplanation;

    // 핵심 증거 ID 목록 (쉼표 구분 문자열, 예: "2,6,7,8")
    @Column(name = "key_evidence_ids", columnDefinition = "TEXT")
    private String keyEvidenceIds;

    @Column(name = "proof_dimension_json", columnDefinition = "TEXT")
    private String proofDimensionJson;

    @Column(name = "final_feedback_json", columnDefinition = "TEXT")
    private String finalFeedbackJson;

    @Builder
    private VariantSolution(Long variantId, Long culpritSuspectId, String culpritCode, String culpritName,
                            String culpritRole, String motive, String method,
                            String coverUp, String fullExplanation, String keyEvidenceIds,
                            String proofDimensionJson, String finalFeedbackJson) {
        this.variantId = variantId;
        this.culpritSuspectId = culpritSuspectId;
        this.culpritCode = culpritCode;
        this.culpritName = culpritName;
        this.culpritRole = culpritRole;
        this.motive = motive;
        this.method = method;
        this.coverUp = coverUp;
        this.fullExplanation = fullExplanation;
        this.keyEvidenceIds = keyEvidenceIds;
        this.proofDimensionJson = proofDimensionJson;
        this.finalFeedbackJson = finalFeedbackJson;
    }

    // "2,6,7,8" 형태의 문자열을 List<Long>으로 파싱
    public java.util.List<Long> parseKeyEvidenceIds() {
        if (keyEvidenceIds == null || keyEvidenceIds.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(keyEvidenceIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
    }
}
