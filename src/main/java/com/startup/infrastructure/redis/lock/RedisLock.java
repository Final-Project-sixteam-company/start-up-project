package com.startup.infrastructure.redis.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
// 메서드 단위 분산락 annotation이다. key는 SpEL로 파라미터를 조합할 수 있다.
public @interface RedisLock {

    String key();

    long waitTime() default 5;

    long leaseTime() default 10;

    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
