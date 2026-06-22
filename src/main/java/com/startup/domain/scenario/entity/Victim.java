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
@Table(name = "victims")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
// 사건 피해자 정보. 시나리오당 1명으로 시작하되 구조상 1:N 허용.
public class Victim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "found_location_id")
    private Long foundLocationId;

    @Column(nullable = false, length = 100)
    private String name;

    private Integer age;

    @Column(length = 100)
    private String role;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cause_of_death", length = 255)
    private String causeOfDeath;

    @Column(name = "found_condition", columnDefinition = "TEXT")
    private String foundCondition; //발견 당시 상태

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Victim(Long scenarioId, Long foundLocationId, String name, Integer age,
                   String role, String description, String causeOfDeath, String foundCondition) {
        this.scenarioId = scenarioId;
        this.foundLocationId = foundLocationId;
        this.name = name;
        this.age = age;
        this.role = role;
        this.description = description;
        this.causeOfDeath = causeOfDeath;
        this.foundCondition = foundCondition;
    }

    public void updateInfo(Long foundLocationId, String name, Integer age, String role,
                           String description, String causeOfDeath, String foundCondition) {
        this.foundLocationId = foundLocationId;
        this.name = name;
        this.age = age;
        this.role = role;
        this.description = description;
        this.causeOfDeath = causeOfDeath;
        this.foundCondition = foundCondition;
    }

}
