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
// MVP 단계에서는 is_active=true인 SECRETARY 1개만 운영한다.
public class ScenarioVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_type", nullable = false, length = 30)
    private VariantType variantType;

    @Column(name = "variant_name", nullable = false, length = 100)
    private String variantName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false; // MVP: SECRETARY만 true

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder
    private ScenarioVariant(Long scenarioId, VariantType variantType,
                            String variantName, String description,
                            Boolean isActive, Integer sortOrder) {
        this.scenarioId = scenarioId;
        this.variantType = variantType;
        this.variantName = variantName;
        this.description = description;
        this.isActive = isActive != null ? isActive : false;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }
}
