package com.startup.domain.play.support;

import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.support.PlaySessionCompleter;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.service.PlaySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Component
@RequiredArgsConstructor
public class DefaultPlaySessionCompleter implements PlaySessionCompleter {

    private final PlaySessionService playSessionService;
    private final PlaySessionRepository playSessionRepository;
    private final FinalDeductionLockManager lockManager;

    @Override
    public void lockForFinalDeduction(Long sessionId) {
        // 이미 완료된 세션인지 DB에서 실제 확인
        playSessionRepository.findById(sessionId).ifPresent(session -> {
            if (!session.isPlaying()) {
                throw new AiException(AiErrorCode.FINAL_DEDUCTION_ALREADY_SUBMITTED);
            }
        });

        // 채점 중복 요청 방지 (In-memory Lock)
        if (!lockManager.tryLock(sessionId)) {
            throw new AiException(AiErrorCode.SCORING_IN_PROGRESS);
        }
    }

    @Override
    public void complete(Long sessionId) {
        try {
            // 실제 우리 DB의 세션을 COMPLETED로 변경
            playSessionService.completeSession(sessionId);
        } finally {
            lockManager.release(sessionId);
        }
    }

    @Override
    public void releaseFinalDeductionLock(Long sessionId) {
        lockManager.release(sessionId);
    }

}
