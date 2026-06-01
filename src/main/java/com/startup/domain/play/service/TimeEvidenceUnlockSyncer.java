package com.startup.domain.play.service;

import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.play.support.EvidenceUnlockPolicy;
import com.startup.domain.scenario.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TimeEvidenceUnlockSyncer {

    private final PlaySessionRepository playSessionRepository;
    private final EvidenceRepository evidenceRepository;
    private final UnlockedEvidenceRepository unlockedEvidenceRepository;
    private final EvidenceUnlockPolicy evidenceUnlockPolicy;

    // REQUIRES_NEW: 외부 readOnly 트랜잭션과 분리된 쓰기 트랜잭션으로 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sync(Long sessionId, Long currentUserId) {
        playSessionRepository.findById(sessionId).ifPresent(session -> {
            //소유자 검증 + playing 상태 확인 후 동기화
            if (!session.isPlaying()) return;
            if (!session.getUserId().equals(currentUserId)) return;

            int elapsedMinutes = (int) Duration.between(session.getStartedAt(), LocalDateTime.now()).toMinutes();
            evidenceRepository.findAllByScenarioIdOrderBySortOrder(session.getScenarioId())
                    .stream()
                    .filter(e -> evidenceUnlockPolicy.canAutoUnlock(e, elapsedMinutes))
                    .forEach(e -> unlockedEvidenceRepository
                            .insertIgnoreUnlockedEvidence(sessionId, e.getId(), e.getUnlockType().name() + "_AUTO"));
        });
    }
}
