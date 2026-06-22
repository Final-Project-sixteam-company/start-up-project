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
@Table(name = "hints")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
// 시나리오 힌트 정보. hintLevel 순서대로 단계적으로 제공된다.
// content는 플레이어가 힌트를 사용(use)한 시점에만 노출해야 한다.
public class Hint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "hint_level", nullable = false)
    private Integer hintLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 해금까지 필요한 경과 시간(분). null이면 즉시 사용 가능.
    @Column(name = "unlock_after_minutes")
    private Integer unlockAfterMinutes;

    // 이 힌트 사용 시 차감되는 점수
    @Column(name = "penalty_score", nullable = false)
    private Integer penaltyScore;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Hint(Long scenarioId, Integer hintLevel, String content,
                 Integer unlockAfterMinutes, Integer penaltyScore) {
        this.scenarioId = scenarioId;
        this.hintLevel = hintLevel;
        this.content = content;
        this.unlockAfterMinutes = unlockAfterMinutes;
        this.penaltyScore = penaltyScore != null ? penaltyScore : 0;
    }
}
