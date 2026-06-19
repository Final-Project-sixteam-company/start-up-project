package com.startup.domain.ai.support;

import com.startup.common.auth.AuthenticatedUserPrincipal;
import com.startup.common.auth.CurrentUserProvider;
import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.dto.AiQuotaStatus;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.auth.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRateLimitService {

    private static final ZoneId RATE_LIMIT_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String KEY_PREFIX = "ai:rate:daily";

    private final StringRedisTemplate redisTemplate;
    private final CurrentUserProvider currentUserProvider;
    private final AiRateLimitProperties properties;

    public AiQuotaStatus checkAndConsume(AiCallContext context) {
        if (!properties.isEnabled()
                || (properties.getDailyLimitPerUserPerScenario() <= 0
                && properties.getDailyLimitPerUserTotal() <= 0)) {
            return null;
        }

        Optional<AuthenticatedUserPrincipal> principal = currentUserProvider.authenticatedPrincipal();
        if (isAdminBypass(principal)) {
            return null;
        }

        String subjectKey = resolveSubjectKey(principal);
        String totalKey = null;
        long accountCount = 0L;
        boolean accountConsumed = false;

        try {
            if (properties.getDailyLimitPerUserTotal() > 0) {
                totalKey = buildTotalKey(subjectKey);
                accountCount = consume(
                        totalKey,
                        properties.getDailyLimitPerUserTotal(),
                        subjectKey,
                        "total",
                        context
                );
                accountConsumed = true;
            }

            if (context.scenarioId() != null && properties.getDailyLimitPerUserPerScenario() > 0) {
                long scenarioCount = consume(
                        buildScenarioKey(subjectKey, context.scenarioId()),
                        properties.getDailyLimitPerUserPerScenario(),
                        subjectKey,
                        "scenario:" + context.scenarioId(),
                        context
                );
                return buildQuotaStatus(scenarioCount, accountCount);
            }

            return null;
        } catch (AiException e) {
            if (accountConsumed && totalKey != null) {
                release(totalKey);
            }
            throw e;
        }
    }

    private boolean isAdminBypass(Optional<AuthenticatedUserPrincipal> principal) {
        return properties.isAdminBypassEnabled()
                && principal.map(AuthenticatedUserPrincipal::role)
                .filter(UserRole.ADMIN::equals)
                .isPresent();
    }

    private String resolveSubjectKey(Optional<AuthenticatedUserPrincipal> principal) {
        Long userId = principal
                .map(AuthenticatedUserPrincipal::userId)
                .orElseGet(currentUserProvider::currentUserIdOrNull);

        if (userId != null) {
            return "user:" + userId;
        }

        return "anonymous";
    }

    private long consume(String key, long limit, String subjectKey, String scope, AiCallContext context) {
        long count = increment(key);
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofHours(properties.getTtlHours()));
        }

        if (count > limit) {
            release(key);
            log.warn("AI rate limit exceeded. subject={}, scope={}, feature={}, count={}, limit={}",
                    subjectKey, scope, context.featureType(), count, limit);
            throw new AiException(AiErrorCode.AI_DAILY_RATE_LIMIT_EXCEEDED);
        }
        return count;
    }

    private AiQuotaStatus buildQuotaStatus(long scenarioCount, long accountCount) {
        Advisory advisory = advisoryFor(scenarioCount);
        long scenarioLimit = properties.getDailyLimitPerUserPerScenario();
        return new AiQuotaStatus(
                "SCENARIO_DAILY",
                scenarioCount,
                scenarioLimit,
                accountCount,
                properties.getDailyLimitPerUserTotal(),
                advisory.stage(),
                advisory.recommendedAction(),
                advisory.message(),
                nextThreshold(scenarioCount),
                Math.max(0L, scenarioLimit - scenarioCount)
        );
    }

    private Advisory advisoryFor(long used) {
        if (used >= 120) {
            return new Advisory(
                    "STRONGLY_RECOMMEND_FINAL_DEDUCTION",
                    "OPEN_FINAL_DEDUCTION",
                    "심문량이 많습니다. 추가 질문보다 증거를 정리하고 최종 추리를 진행해 보세요."
            );
        }
        if (used >= 100) {
            return new Advisory(
                    "SUGGEST_FINAL_DEDUCTION_CHECKLIST",
                    "OPEN_FINAL_DEDUCTION_CHECKLIST",
                    "충분히 많은 심문을 진행했습니다. 범인·동기·수단·은폐 정황을 정리해 보세요."
            );
        }
        if (used >= 70) {
            return new Advisory(
                    "SUGGEST_HINT_OR_GUIDANCE",
                    "OPEN_HINT_OR_GUIDANCE",
                    "증거 상세의 추천 질문, 함께 볼 증거, 힌트를 활용해 남은 모순을 좁혀보세요."
            );
        }
        if (used >= 50) {
            return new Advisory(
                    "SUGGEST_SUSPECT_COMPARE",
                    "OPEN_SUSPECT_TIMELINE_COMPARE",
                    "용의자별 진술과 타임라인을 비교해 후보를 1~2명으로 좁혀보세요."
            );
        }
        if (used >= 35) {
            return new Advisory(
                    "SUGGEST_EVIDENCE_REVIEW",
                    "OPEN_EVIDENCE_TIMELINE",
                    "지금까지 해금한 증거와 타임라인을 한 번 정리해 보세요."
            );
        }
        return new Advisory("NONE", "CONTINUE_INTERROGATION", "");
    }

    private Long nextThreshold(long used) {
        long[] thresholds = {35L, 50L, 70L, 100L, 120L};
        for (long threshold : thresholds) {
            if (used < threshold) {
                return threshold;
            }
        }
        return null;
    }

    private String buildScenarioKey(String subjectKey, Long scenarioId) {
        String date = LocalDate.now(RATE_LIMIT_ZONE).format(DATE_FORMAT);
        return KEY_PREFIX + ":" + date + ":" + subjectKey + ":scenario:" + scenarioId;
    }

    private String buildTotalKey(String subjectKey) {
        String date = LocalDate.now(RATE_LIMIT_ZONE).format(DATE_FORMAT);
        return KEY_PREFIX + ":" + date + ":" + subjectKey + ":total";
    }

    private long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value == null) {
                throw new AiException(AiErrorCode.AI_RATE_LIMIT_UNAVAILABLE);
            }
            return value;
        } catch (RedisConnectionFailureException e) {
            log.error("AI rate limit Redis connection failed", e);
            throw new AiException(AiErrorCode.AI_RATE_LIMIT_UNAVAILABLE, e);
        }
    }

    private void release(String key) {
        try {
            redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.warn("AI rate limit rollback failed. key={}", key, e);
        }
    }

    private record Advisory(String stage, String recommendedAction, String message) {
    }
}
