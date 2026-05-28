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
@Table(
        name = "play_sessions",
        uniqueConstraints = {
                // NULL은 UNIQUE 충돌 안 함 → COMPLETED 세션이 여러 개여도 문제없음
                // PLAYING 상태일 때만 "userId_scenarioId" 값이 들어가 중복 방지
                @UniqueConstraint(
                        name = "uk_play_sessions_active_key",
                        columnNames = {"active_key"}
                )
        }
    )
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

    // PLAYING 상태일 때 "userId_scenarioId" 값 저장, 완료/포기 시 null로 초기화
    @Column(name = "active_key", unique = true)
    private String activeKey;

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
        // PLAYING 시작 시 active_key 설정
        this.activeKey = userId + "_" + scenarioId;
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
        this.activeKey = null; // 완료 시 null로 초기화 → UNIQUE 제약에서 자유로워짐
    }

    public void abandon() {
        //종료 전에 경과시간 먼저 저장
        this.currentElapsedSeconds = (int) Duration.between(this.startedAt, LocalDateTime.now()).getSeconds();

        this.status = PlaySessionStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();
        this.activeKey = null; // 포기 시 null로 초기화
    }

    public boolean isPlaying() {
        return this.status == PlaySessionStatus.PLAYING;
    }

    // PlaySessionCompleter 인터페이스가 score/grade를 받지 않으므로, 세션 상태 전환과 active_key 해제만 처리하는 전용 메서드.
    // 실제 score/grade는 FinalDeduction 테이블에 저장된다.
    public void markCompleted() {
        this.currentElapsedSeconds = (int) Duration.between(this.startedAt, LocalDateTime.now()).getSeconds();
        this.status = PlaySessionStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
        this.activeKey = null; // UNIQUE 제약 해제 → 재플레이 허용
    }
}
