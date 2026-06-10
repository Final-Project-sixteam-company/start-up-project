package com.startup.domain.scenario.entity;

import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "evidences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
// 증거 카드. 시나리오 정적 데이터이며, 플레이 중 노출 여부는 UnlockedEvidence로 제어한다.
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(length = 100)
    private String code;

    @Column(name = "location_id")
    private Long locationId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "one_line", length = 500)
    private String oneLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 50)
    private EvidenceType evidenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvidenceImportance importance;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_asset_key", length = 500)
    private String imageAssetKey;

    @Column(name = "thumbnail_asset_key", length = 500)
    private String thumbnailAssetKey;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @Column(name = "guidance_json", columnDefinition = "TEXT")
    private String guidanceJson;

    @Column(name = "unlock_phase", length = 50)
    private String unlockPhase;

    @Column(name = "is_initial_public", nullable = false)
    private Boolean isInitialPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "unlock_type", nullable = false, length = 50)
    private EvidenceUnlockType unlockType;

    @Column(name = "unlock_condition_json", columnDefinition = "TEXT")
    private String unlockConditionJson;

    @Column(name = "unlock_after_minutes")
    private Integer unlockAfterMinutes;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Evidence(Long scenarioId, String code, Long locationId, String title, String description,
                     String oneLine, EvidenceType evidenceType, EvidenceImportance importance,
                     String imageUrl, String imageAssetKey, String thumbnailAssetKey, String tagsJson,
                     String guidanceJson,
                     String unlockPhase,
                     Boolean isInitialPublic, EvidenceUnlockType unlockType, String unlockConditionJson,
                     Integer unlockAfterMinutes, Integer sortOrder) {
        this.scenarioId = scenarioId;
        this.code = code;
        this.locationId = locationId;
        this.title = title;
        this.description = description;
        this.oneLine = oneLine;
        this.evidenceType = evidenceType != null ? evidenceType : EvidenceType.SCENE;
        this.importance = importance != null ? importance : EvidenceImportance.NORMAL;
        this.imageUrl = imageUrl;
        this.imageAssetKey = imageAssetKey;
        this.thumbnailAssetKey = thumbnailAssetKey;
        this.tagsJson = tagsJson;
        this.guidanceJson = guidanceJson;
        this.unlockPhase = unlockPhase;
        this.isInitialPublic = isInitialPublic != null ? isInitialPublic : false;
        this.unlockType = unlockType != null ? unlockType : EvidenceUnlockType.NONE;
        this.unlockConditionJson = unlockConditionJson;
        this.unlockAfterMinutes = unlockAfterMinutes;
        this.sortOrder = sortOrder;
    }
}
