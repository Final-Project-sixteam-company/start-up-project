package com.startup.domain.play.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "unlocked_evidences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_unlocked_evidences_session_evidence",
                        columnNames = {"play_session_id", "evidence_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 플레이 세션별 해금된 증거 기록. 사용자가 현재 볼 수 있는 증거를 판단하는 기준이다.
public class UnlockedEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "play_session_id", nullable = false)
    private Long playSessionId;

    @Column(name = "evidence_id", nullable = false)
    private Long evidenceId;

    @Column(name = "unlocked_reason", length = 255)
    private String unlockedReason;

    @Column(name = "unlocked_at", nullable = false)
    private LocalDateTime unlockedAt;

    @Builder
    private UnlockedEvidence(Long playSessionId, Long evidenceId, String unlockedReason) {
        this.playSessionId = playSessionId;
        this.evidenceId = evidenceId;
        this.unlockedReason = unlockedReason;
        this.unlockedAt = LocalDateTime.now();
    }
}
