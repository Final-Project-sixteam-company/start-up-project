package com.startup.infrastructure.redis.lock;

import java.util.concurrent.TimeUnit;

// 락 구현체를 Redisson 외 다른 방식으로 바꿔도 Aspect 코드는 유지하기 위한 인터페이스다.
public interface LockService {

    void lock(String key, long waitTime, long leaseTime, TimeUnit timeUnit);

    void unlock(String key);
}
