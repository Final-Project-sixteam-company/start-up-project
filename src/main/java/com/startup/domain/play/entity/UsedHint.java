package com.startup.domain.play.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "used_hints")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 플레이어가 실제로 사용(열람)한 힌트 기록.
// 사용 여부 추적 및 중복 사용 방지를 위해 play_session_id + hint_id 에 unique 제약이 걸려 있다.
public class UsedHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "play_session_id", nullable = false)
    private Long playSessionId;

    @Column(name = "hint_id", nullable = false)
    private Long hintId;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Builder
    private UsedHint(Long playSessionId, Long hintId) {
        this.playSessionId = playSessionId;
        this.hintId = hintId;
        this.usedAt = LocalDateTime.now();
    }
}
