package com.startup.infrastructure.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
// @RedisLock이 붙은 메서드 실행 전후로 Redisson 분산락을 획득/해제한다.
public class RedisLockAspect {

    private final LockService lockService;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RedisLockAspect(@Qualifier("redissonLockService") LockService lockService) {
        this.lockService = lockService;
    }

    @Around("@annotation(redisLock)")
    public Object around(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        String key = resolveKey(joinPoint, redisLock.key());

        lockService.lock(key, redisLock.waitTime(), redisLock.leaseTime(), redisLock.timeUnit());
        try {
            return joinPoint.proceed();
        } finally {
            lockService.unlock(key);
        }
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        // SpEL에서 #memberId, #p0, #a0 같은 방식으로 메서드 파라미터를 참조할 수 있게 한다.
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int index = 0; index < args.length; index++) {
            context.setVariable("p" + index, args[index]);
            context.setVariable("a" + index, args[index]);
            if (parameterNames != null && parameterNames.length > index) {
                context.setVariable(parameterNames[index], args[index]);
            }
        }

        String key = parser.parseExpression(keyExpression).getValue(context, String.class);
        if (!StringUtils.hasText(key)) {
            throw new LockException("Redis lock key must not be blank.");
        }
        return key;
    }
}
