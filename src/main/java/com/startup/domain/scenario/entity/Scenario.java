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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "scenarios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE scenarios SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Scenario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

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

    private Integer suspectCount = 0;
    private Integer evidenceCount = 0;
    private Integer hintCount = 0;

    private Integer playCount = 0;
    private Double averageRating = 0.0;
    private Integer ratingCount = 0;

    @Column(name = "price_credit")
    private Integer priceCredit = 0;

    @Column(nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScenarioStatus status;

    @Builder
    public Scenario(String title, String description, String synopsis, ScenarioType scenarioType, ScenarioVisibility visibility, Difficulty difficulty, Integer estimatedPlayTimeMinutes, Integer playerCountMin, Integer playerCountMax, Integer priceCredit, Long creatorId, ScenarioStatus status) {
        this.title = title;
        this.description = description;
        this.synopsis = synopsis;
        this.scenarioType = scenarioType;
        this.visibility = visibility;
        this.difficulty = difficulty;
        this.estimatedPlayTimeMinutes = estimatedPlayTimeMinutes;
        this.playerCountMin = playerCountMin;
        this.playerCountMax = playerCountMax;
        this.priceCredit = priceCredit != null ? priceCredit : 0;
        this.creatorId = creatorId;
        this.status = status != null ? status : ScenarioStatus.DRAFT;
        this.playCount = 0;
        this.averageRating = 0.0;
        this.ratingCount = 0;
        this.suspectCount = 0;
        this.evidenceCount = 0;
        this.hintCount = 0;
    }
}
