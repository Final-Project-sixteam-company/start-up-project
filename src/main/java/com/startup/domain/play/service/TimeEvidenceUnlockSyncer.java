package com.startup.domain.play.service;

import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.play.support.EvidenceUnlockPolicy;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceUnlockRule;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.EvidenceUnlockRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeEvidenceUnlockSyncer {

    private final PlaySessionRepository playSessionRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
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
            List<Evidence> scenarioEvidences =
                    evidenceRepository.findAllByScenarioIdOrderBySortOrder(session.getScenarioId());
            Set<Long> alreadyUnlockedIds = unlockedEvidenceRepository.findAllByPlaySessionId(sessionId)
                    .stream()
                    .map(UnlockedEvidence::getEvidenceId)
                    .collect(Collectors.toSet());
            Set<String> unlockedEvidenceCodes = scenarioEvidences.stream()
                    .filter(evidence -> alreadyUnlockedIds.contains(evidence.getId()))
                    .map(Evidence::getCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, EvidenceUnlockRule> unlockRulesByEvidenceId = evidenceUnlockRuleRepository
                    .findAllByScenarioIdOrderBySortOrder(session.getScenarioId())
                    .stream()
                    .collect(Collectors.toMap(
                            EvidenceUnlockRule::getEvidenceId,
                            Function.identity(),
                            (left, right) -> left
                    ));

            scenarioEvidences
                    .stream()
                    .filter(e -> !alreadyUnlockedIds.contains(e.getId()))
                    .filter(e -> evidenceUnlockPolicy.canAutoUnlock(
                            e,
                            unlockRulesByEvidenceId.get(e.getId()),
                            elapsedMinutes,
                            unlockedEvidenceCodes
                    ))
                    .forEach(e -> unlockedEvidenceRepository
                            .insertIgnoreUnlockedEvidence(sessionId, e.getId(), e.getUnlockType().name() + "_AUTO"));
        });
    }
}
