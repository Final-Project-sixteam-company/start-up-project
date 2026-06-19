package com.startup.domain.ai.support;

import com.startup.common.auth.AuthenticatedUserPrincipal;
import com.startup.common.auth.CurrentUserProvider;
import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.enums.AiFeatureType;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.auth.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRateLimitServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private CurrentUserProvider currentUserProvider;
    private AiRateLimitProperties properties;
    private AiRateLimitService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        properties = new AiRateLimitProperties();
        properties.setDailyLimitPerUserPerScenario(150);
        properties.setDailyLimitPerUserTotal(350);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new AiRateLimitService(redisTemplate, currentUserProvider, properties);
    }

    @Test
    void checkAndConsume_allowsWithinDailyLimitAndSetsTtlOnFirstUse() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(userPrincipal(10L)));
        when(valueOperations.increment(startsWith("ai:rate:daily:"))).thenReturn(1L, 1L);
        when(redisTemplate.expire(startsWith("ai:rate:daily:"), any(Duration.class))).thenReturn(true);

        service.checkAndConsume(context());

        verify(redisTemplate, times(2)).expire(startsWith("ai:rate:daily:"), any(Duration.class));
    }

    @Test
    void checkAndConsume_rollsBackAndThrowsRateLimitUnavailableWhenExpireFails() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(userPrincipal(10L)));
        when(valueOperations.increment(startsWith("ai:rate:daily:"))).thenReturn(1L);
        when(redisTemplate.expire(startsWith("ai:rate:daily:"), any(Duration.class)))
                .thenThrow(new QueryTimeoutException("redis timeout"));

        assertThatThrownBy(() -> service.checkAndConsume(context()))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_RATE_LIMIT_UNAVAILABLE));

        verify(valueOperations).decrement(startsWith("ai:rate:daily:"));
    }

    @Test
    void checkAndConsume_translatesRedisCommandFailuresToRateLimitUnavailable() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(userPrincipal(10L)));
        when(valueOperations.increment(startsWith("ai:rate:daily:")))
                .thenThrow(new QueryTimeoutException("redis timeout"));

        assertThatThrownBy(() -> service.checkAndConsume(context()))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_RATE_LIMIT_UNAVAILABLE));
    }

    @Test
    void checkAndConsume_allowsSecondScenarioWhenAccountTotalIsBelowLimit() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(userPrincipal(10L)));
        when(valueOperations.increment(startsWith("ai:rate:daily:"))).thenReturn(300L, 150L);

        service.checkAndConsume(context());
    }

    @Test
    void checkAndConsume_blocksWhenScenarioDailyLimitExceeded() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(userPrincipal(10L)));
        when(valueOperations.increment(startsWith("ai:rate:daily:"))).thenReturn(151L);

        assertThatThrownBy(() -> service.checkAndConsume(context()))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(AiErrorCode.AI_DAILY_RATE_LIMIT_EXCEEDED));
    }

    @Test
    void checkAndConsume_rollsBackTotalQuotaWhenScenarioLimitRejects() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(userPrincipal(10L)));
        when(valueOperations.increment(startsWith("ai:rate:daily:"))).thenReturn(200L, 151L);

        assertThatThrownBy(() -> service.checkAndConsume(context()))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(AiErrorCode.AI_DAILY_RATE_LIMIT_EXCEEDED));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(2)).decrement(keyCaptor.capture());
        assertThat(keyCaptor.getAllValues()).anySatisfy(key -> assertThat(key).contains(":total"));
        assertThat(keyCaptor.getAllValues()).anySatisfy(key -> assertThat(key).contains(":scenario:1"));
    }

    @Test
    void checkAndConsume_blocksWhenAccountTotalDailyLimitExceeded() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(userPrincipal(10L)));
        when(valueOperations.increment(startsWith("ai:rate:daily:"))).thenReturn(351L);

        assertThatThrownBy(() -> service.checkAndConsume(context()))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(AiErrorCode.AI_DAILY_RATE_LIMIT_EXCEEDED));
    }

    @Test
    void checkAndConsume_returnsAdvisoryAtThresholds() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(userPrincipal(10L)));
        when(valueOperations.increment(startsWith("ai:rate:daily:"))).thenReturn(90L, 70L);

        var status = service.checkAndConsume(context());

        assertThat(status.scenarioUsed()).isEqualTo(70L);
        assertThat(status.scenarioLimit()).isEqualTo(150L);
        assertThat(status.accountUsed()).isEqualTo(90L);
        assertThat(status.accountLimit()).isEqualTo(350L);
        assertThat(status.stage()).isEqualTo("SUGGEST_HINT_OR_GUIDANCE");
        assertThat(status.recommendedAction()).isEqualTo("OPEN_HINT_OR_GUIDANCE");
        assertThat(status.nextThreshold()).isEqualTo(100L);
        assertThat(status.remaining()).isEqualTo(80L);
    }

    @Test
    void checkAndConsume_skipsAdminWhenBypassEnabled() {
        when(currentUserProvider.authenticatedPrincipal())
                .thenReturn(Optional.of(new AuthenticatedUserPrincipal(
                        1L, "admin@example.com", UserRole.ADMIN)));

        service.checkAndConsume(context());

        verify(redisTemplate, never()).opsForValue();
    }

    private AuthenticatedUserPrincipal userPrincipal(Long userId) {
        return new AuthenticatedUserPrincipal(userId, "user@example.com", UserRole.USER);
    }

    private AiCallContext context() {
        return new AiCallContext(
                AiFeatureType.INTERROGATION,
                "npc_interrogation_v1",
                1L,
                2L,
                3L,
                "NPC_SECRETARY"
        );
    }
}
