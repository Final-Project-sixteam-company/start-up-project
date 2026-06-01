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
        name = "evidence_variant_states",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_evidence_variant_states_variant_evidence", columnNames = {"variant_id", "evidence_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// activeVariant에 따라 같은 증거 카드에 덧붙일 player-safe 상세와 내부 채점 역할을 저장한다.
public class EvidenceVariantState extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "evidence_id", nullable = false)
    private Long evidenceId;

    @Column(name = "variant_code", nullable = false, length = 100)
    private String variantCode;

    @Column(name = "evidence_code", nullable = false, length = 100)
    private String evidenceCode;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "detail_override", columnDefinition = "TEXT")
    private String detailOverride;

    @Column(name = "detail_append", columnDefinition = "TEXT")
    private String detailAppend;

    @Column(name = "proof_dimensions_json", columnDefinition = "TEXT")
    private String proofDimensionsJson;

    @Builder
    private EvidenceVariantState(Long scenarioId, Long variantId, Long evidenceId,
                                 String variantCode, String evidenceCode, String role,
                                 String detailOverride, String detailAppend,
                                 String proofDimensionsJson) {
        this.scenarioId = scenarioId;
        this.variantId = variantId;
        this.evidenceId = evidenceId;
        this.variantCode = variantCode;
        this.evidenceCode = evidenceCode;
        this.role = role;
        this.detailOverride = detailOverride;
        this.detailAppend = detailAppend;
        this.proofDimensionsJson = proofDimensionsJson;
    }
}
