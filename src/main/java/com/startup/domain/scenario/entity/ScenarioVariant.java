package com.startup.domain.scenario.entity;

import com.startup.common.entity.BaseEntity;
import com.startup.domain.scenario.enums.VariantType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "scenario_variants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// Scenario 1 : N ScenarioVariant.
// 하나의 시나리오에서 범인이 달라지는 변주(Variant)를 관리한다.
// 공식 시나리오의 경우 여러 Variant가 동시에 활성화될 수 있으며, 플레이 세션 생성 시 가중치(weight)를 기반으로 랜덤 샘플링된다.
public class ScenarioVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", nullable = false, length = 30)
    private VariantType variantType;

    @Column(name = "variant_name", nullable = false, length = 100)
    private String variantName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "culprit_code", length = 100)
    private String culpritCode;

    @Column(nullable = false)
    private Integer weight = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false; // 다중 활성화 지원 (가중치 기반 랜덤 샘플링)

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;


    @Builder
    private ScenarioVariant(Long scenarioId, String code, VariantType variantType,
                            String variantName, String description, String culpritCode,
                            Integer weight, Boolean isActive, Integer sortOrder) {
        this.scenarioId = scenarioId;
        this.code = code;
        this.variantType = variantType;
        this.variantName = variantName;
        this.description = description;
        this.culpritCode = culpritCode;
        this.weight = weight != null ? weight : 1;
        this.isActive = isActive != null ? isActive : false;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    //활성화 상태 변경
    public void activate() {
        this.isActive = true;
    }
    public void deactivate() {
        this.isActive = false;
    }
}
