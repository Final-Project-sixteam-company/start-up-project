package com.startup.domain.ai.support;

import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockPlaySessionCompleter implements PlaySessionCompleter {

    private final Set<Long> lockedSessions = ConcurrentHashMap.newKeySet();
    private final Set<Long> completedSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void lockForFinalDeduction(Long sessionId) {
        if (completedSessions.contains(sessionId)) {
            throw new AiException(AiErrorCode.FINAL_DEDUCTION_ALREADY_SUBMITTED);
        }
        lockedSessions.add(sessionId);
    }

    @Override
    public void complete(Long sessionId) {
        lockedSessions.remove(sessionId);
        completedSessions.add(sessionId);
    }
}
