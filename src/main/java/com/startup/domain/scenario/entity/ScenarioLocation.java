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

    @Column(nullable = false, length = 100)
    private String name; //장소명

    @Column(columnDefinition = "TEXT")
    private String description;

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
    private ScenarioLocation(Long scenarioId, String name, String description,
                             Integer mapX, Integer mapY, Integer sortOrder) {
        this.scenarioId = scenarioId;
        this.name = name;
        this.description = description;
        this.mapX = mapX;
        this.mapY = mapY;
        this.sortOrder = sortOrder;
    }
}
