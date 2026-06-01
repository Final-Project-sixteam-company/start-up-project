package com.startup.domain.play.support;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 최종 추리 채점 중(in-flight) 세션 락을 관리하는 독립 빈.
 * PlaySessionService와 DefaultPlaySessionCompleter 양쪽에서 사용하며,
 * 순환 의존성을 피하기 위해 별도 클래스로 분리한다.
 */
@Component
public class FinalDeductionLockManager {

    private final Set<Long> lockedSessions = ConcurrentHashMap.newKeySet();

    public boolean tryLock(Long sessionId) {
        return lockedSessions.add(sessionId);
    }

    public void release(Long sessionId) {
        lockedSessions.remove(sessionId);
    }

    public boolean isLocked(Long sessionId) {
        return lockedSessions.contains(sessionId);
    }
}
