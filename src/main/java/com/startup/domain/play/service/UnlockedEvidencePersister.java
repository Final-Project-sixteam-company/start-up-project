package com.startup.domain.play.service;

import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnlockedEvidencePersister {

    private final UnlockedEvidenceRepository unlockedEvidenceRepository;

    /**
     * 시간 기반 증거 해금 INSERT.
     * REQUIRES_NEW: 호출자 트랜잭션과 분리하여 즉시 flush/commit하므로
     * unique constraint 위반이 즉시 DataIntegrityViolationException으로 전파된다.
     * 동시 요청 경쟁 시 두 번째 요청이 정상적으로 예외를 catch할 수 있도록 보장.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveIfNotExists(Long sessionId, Long evidenceId, String reason){
        unlockedEvidenceRepository.save(
                UnlockedEvidence.builder()
                        .playSessionId(sessionId)
                        .evidenceId(evidenceId)
                        .unlockedReason(reason)
                        .build()
        );
        log.info("시간 기반 증거 자동 해금: sessionId={}, evidenceId={}, reason={}", sessionId, evidenceId, reason);
    }
}
