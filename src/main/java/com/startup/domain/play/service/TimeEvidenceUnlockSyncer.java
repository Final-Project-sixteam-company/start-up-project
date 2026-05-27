package com.startup.domain.play.service;

import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
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

    // REQUIRES_NEW: 외부 readOnly 트랜잭션과 분리된 쓰기 트랜잭션으로 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sync(Long sessionId) {
        playSessionRepository.findById(sessionId).ifPresent(session -> {
            int elapsedMinutes = (int) Duration.between(session.getStartedAt(), LocalDateTime.now()).toMinutes();
            evidenceRepository.findAllByScenarioIdOrderBySortOrder(session.getScenarioId())
                    .stream()
                    .filter(e -> !e.getIsInitialPublic())
                    .filter(e -> EvidenceUnlockType.TIME == e.getUnlockType())
                    .filter(e -> e.getUnlockAfterMinutes() != null)
                    .filter(e -> elapsedMinutes >= e.getUnlockAfterMinutes())
                    .forEach(e -> unlockedEvidenceRepository
                            .insertIgnoreUnlockedEvidence(sessionId, e.getId(), "TIME"));
        });
    }
}
