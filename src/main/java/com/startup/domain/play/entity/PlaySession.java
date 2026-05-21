package com.startup.domain.play.entity;

import com.startup.common.entity.BaseEntity;
import com.startup.domain.play.enums.PlaySessionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "play_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 사용자별 시나리오 플레이 진행 상태를 관리하는 Aggregate Root.
// 증거 해금, 힌트 사용, 심문, 최종 추리는 모두 이 세션 기준으로 추적한다.
public class PlaySession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlaySessionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "current_elapsed_seconds", nullable = false)
    private Integer currentElapsedSeconds = 0; //현재 진행 시간

    @Column(nullable = false)
    private Integer score = 0;

    @Column(length = 20)
    private String grade;

    @Column(name = "hint_count", nullable = false)
    private Integer hintCount = 0; //사용 힌트 수

    @Column(name = "interrogation_count", nullable = false)
    private Integer interrogationCount = 0; //심문 횟수

    @Builder
    private PlaySession(Long userId, Long scenarioId) {
        this.userId = userId;
        this.scenarioId = scenarioId;
        this.status = PlaySessionStatus.PLAYING;
        this.startedAt = LocalDateTime.now();
        this.currentElapsedSeconds = 0;
        this.score = 0;
        this.hintCount = 0;
        this.interrogationCount = 0;
    }

    public void incrementInterrogationCount() {
        this.interrogationCount++;
    }

    public void incrementHintCount() {
        this.hintCount++;
    }

    public void complete(int score, String grade) {
        //종료 전에 경과시간 먼저 저장
        this.currentElapsedSeconds = (int) Duration.between(this.startedAt, LocalDateTime.now()).getSeconds();

        this.status = PlaySessionStatus.COMPLETED;
        this.score = score;
        this.grade = grade;
        this.endedAt = LocalDateTime.now();
    }

    public void abandon() {
        //종료 전에 경과시간 먼저 저장
        this.currentElapsedSeconds = (int) Duration.between(this.startedAt, LocalDateTime.now()).getSeconds();

        this.status = PlaySessionStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();
    }

    public boolean isPlaying() {
        return this.status == PlaySessionStatus.PLAYING;
    }
}
