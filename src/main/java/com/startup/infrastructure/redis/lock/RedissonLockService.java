package com.startup.infrastructure.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component("redissonLockService")
// Redisson RLock으로 실제 분산락을 처리하는 구현체다.
public class RedissonLockService implements LockService {

    private final RedissonClient redissonClient;

    public RedissonLockService(@Lazy RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(key);
        try {
            // leaseTime이 음수면 Redisson watchdog이 락 만료를 자동 연장한다.
            boolean acquired = leaseTime < 0
                    ? lock.tryLock(waitTime, timeUnit)
                    : lock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                throw new LockException("Redis lock acquisition failed. key=" + key);
            }
            log.debug("Redis lock acquired. key={}", key);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LockException("Redis lock acquisition interrupted. key=" + key, exception);
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        // 다른 스레드가 가진 락을 실수로 해제하지 않도록 현재 스레드 소유 여부를 확인한다.
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.debug("Redis lock released. key={}", key);
        }
    }
}
