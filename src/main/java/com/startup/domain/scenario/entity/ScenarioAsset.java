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
        name = "scenario_assets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_scenario_assets_asset_key", columnNames = {"asset_key"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScenarioAsset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "asset_key", nullable = false, length = 500)
    private String assetKey;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "target_kind", nullable = false, length = 50)
    private String targetKind;

    @Column(name = "target_code", nullable = false, length = 100)
    private String targetCode;

    @Column(name = "s3_object_key", length = 500)
    private String s3ObjectKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "source_status", length = 50)
    private String sourceStatus;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Builder
    private ScenarioAsset(Long scenarioId, String assetKey, String type, String targetKind,
                          String targetCode, String s3ObjectKey, String contentType,
                          String sourceStatus, String altText) {
        this.scenarioId = scenarioId;
        this.assetKey = assetKey;
        this.type = type;
        this.targetKind = targetKind;
        this.targetCode = targetCode;
        this.s3ObjectKey = s3ObjectKey;
        this.contentType = contentType;
        this.sourceStatus = sourceStatus;
        this.altText = altText;
    }
}
