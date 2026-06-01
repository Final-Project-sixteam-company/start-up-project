package com.startup.domain.scenario.entity;

import com.startup.common.entity.BaseEntity;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity
@Table(
        name = "scenarios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_scenarios_code_version", columnNames = {"code", "content_version"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Scenario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String code;

    @Column(name = "content_version", length = 50)
    private String version;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(length = 100)
    private String genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScenarioVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    private Integer estimatedPlayTimeMinutes;
    private Integer playerCountMin;
    private Integer playerCountMax;

    private Integer playCount = 0;
    private Double averageRating = 0.0;
    private Integer ratingCount = 0;

    @Column(name = "price_credit")
    private Integer priceCredit = 0;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "culprit_mode", length = 50)
    private String culpritMode;

    @Column(name = "deduction_mode", length = 50)
    private String deductionMode;

    @Column(name = "map_mode", length = 50)
    private String mapMode;

    @Column(name = "evidence_mode", length = 50)
    private String evidenceMode;

    @Column(name = "cover_asset_key", length = 500)
    private String coverAssetKey;

    @Column(name = "map_asset_key", length = 500)
    private String mapAssetKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScenarioStatus status;

    @Builder
    public Scenario(String code, String version, String contentHash,
                    String title, String description, String synopsis, String genre,
                    ScenarioType scenarioType, ScenarioVisibility visibility, Difficulty difficulty,
                    Integer estimatedPlayTimeMinutes, Integer playerCountMin, Integer playerCountMax,
                    Integer priceCredit, Long creatorId, String culpritMode, String deductionMode,
                    String mapMode, String evidenceMode, String coverAssetKey, String mapAssetKey,
                    ScenarioStatus status) {
        this.code = code;
        this.version = version;
        this.contentHash = contentHash;
        this.title = title;
        this.description = description;
        this.synopsis = synopsis;
        this.genre = genre;
        this.scenarioType = scenarioType;
        this.visibility = visibility;
        this.difficulty = difficulty;
        this.estimatedPlayTimeMinutes = estimatedPlayTimeMinutes;
        this.playerCountMin = playerCountMin;
        this.playerCountMax = playerCountMax;
        this.priceCredit = priceCredit != null ? priceCredit : 0;
        this.creatorId = creatorId;
        this.culpritMode = culpritMode;
        this.deductionMode = deductionMode;
        this.mapMode = mapMode;
        this.evidenceMode = evidenceMode;
        this.coverAssetKey = coverAssetKey;
        this.mapAssetKey = mapAssetKey;
        this.status = status != null ? status : ScenarioStatus.DRAFT;
        this.playCount = 0;
        this.averageRating = 0.0;
        this.ratingCount = 0;
    }

    public void incrementPlayCount(){
        this.playCount++;
    }
}
