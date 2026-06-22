package com.startup.domain.scenario.entity;

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
@Table(name = "scenario_locations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
// 시나리오 내 사건 장소를 표현한다. 증거와 피해자 발견 위치가 이 장소에 연결될 수 있다.
public class ScenarioLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name; //장소명

    @Column(length = 30)
    private String floor;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_asset_key", length = 500)
    private String imageAssetKey;

    @Column(name = "map_x")
    private Integer mapX;

    @Column(name = "map_y")
    private Integer mapY;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder; //표시 순서

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ScenarioLocation(Long scenarioId, String code, String name, String floor,
                             String description, String imageAssetKey,
                             Integer mapX, Integer mapY, Integer sortOrder) {
        this.scenarioId = scenarioId;
        this.code = code;
        this.name = name;
        this.floor = floor;
        this.description = description;
        this.imageAssetKey = imageAssetKey;
        this.mapX = mapX;
        this.mapY = mapY;
        this.sortOrder = sortOrder;
    }
}
