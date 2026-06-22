package com.startup.domain.play.support;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 최종 추리 채점 중(in-flight) 세션 락을 관리하는 독립 빈.
 * PlaySessionService와 DefaultPlaySessionCompleter 양쪽에서 사용하며,
 * 순환 의존성을 피하기 위해 별도 클래스로 분리한다.
 */
@Component
public class FinalDeductionLockManager {

    private final ConcurrentMap<Long, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    public boolean tryLock(Long sessionId) {
        return lockFor(sessionId).writeLock().tryLock();
    }

    public void release(Long sessionId) {
        lockFor(sessionId).writeLock().unlock();
    }

    public boolean isLocked(Long sessionId) {
        return lockFor(sessionId).isWriteLocked();
    }

    public InterrogationLock tryLockInterrogation(Long sessionId) {
        ReentrantReadWriteLock sessionLock = lockFor(sessionId);
        if (sessionLock.isWriteLocked()) {
            return null;
        }

        Lock readLock = sessionLock.readLock();
        return readLock.tryLock()
                ? new InterrogationLock(readLock)
                : null;
    }

    private ReentrantReadWriteLock lockFor(Long sessionId) {
        return locks.computeIfAbsent(sessionId, ignored -> new ReentrantReadWriteLock());
    }

    public static final class InterrogationLock implements AutoCloseable {

        private final Lock lock;

        private InterrogationLock(Lock lock) {
            this.lock = lock;
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }
}
