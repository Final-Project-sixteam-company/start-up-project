package com.startup.domain.ai.client;

import com.startup.domain.ai.enums.AiFeatureType;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.support.AiCallRecorder;
import com.startup.domain.ai.support.AiRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiClientTest {

    @Test
    void recordFallback_preservesProvidedLatency() {
        MockResponseProvider mockResponseProvider = mock(MockResponseProvider.class);
        AiCallRecorder recorder = mock(AiCallRecorder.class);
        AiRateLimitService rateLimitService = mock(AiRateLimitService.class);
        AiClient client = new AiClient(null, mockResponseProvider, recorder, rateLimitService,
                "none", "", "MOCK");
        AiCallContext context = new AiCallContext(
                AiFeatureType.INTERROGATION,
                "npc_interrogation_v1",
                10L,
                20L,
                30L,
                "NPC_SECRETARY"
        );

        client.recordFallback(context, "AI001", 4321L);

        verify(recorder).record(context, "fallback", "FALLBACK",
                4321L, true, "AI001", true, null);
    }

    @Test
    void recordFallback_clampsNegativeLatencyToZero() {
        MockResponseProvider mockResponseProvider = mock(MockResponseProvider.class);
        AiCallRecorder recorder = mock(AiCallRecorder.class);
        AiRateLimitService rateLimitService = mock(AiRateLimitService.class);
        AiClient client = new AiClient(null, mockResponseProvider, recorder, rateLimitService,
                "none", "", "MOCK");
        AiCallContext context = AiCallContext.unknown();

        client.recordFallback(context, "AI001", -1L);

        verify(recorder).record(context, "fallback", "FALLBACK",
                0L, true, "AI001", true, null);
    }

    @Test
    void chatWithMetadata_doesNotConsumeQuotaWhenChatModelMissing() {
        MockResponseProvider mockResponseProvider = mock(MockResponseProvider.class);
        AiCallRecorder recorder = mock(AiCallRecorder.class);
        AiRateLimitService rateLimitService = mock(AiRateLimitService.class);
        AiClient client = new AiClient(null, mockResponseProvider, recorder, rateLimitService,
                "openai", "https://api.deepseek.com", "deepseek-v4-flash");
        AiCallContext context = AiCallContext.unknown();

        assertThatThrownBy(() -> client.chatWithMetadata(
                "system", "user", AiRequestParams.interrogation(0.3, 150), context))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_SERVICE_UNAVAILABLE));

        verify(rateLimitService, never()).checkAndConsume(any(AiCallContext.class));
    }

    @Test
    void chatWithMetadata_recordsQuotaBlockSeparatelyWhenRateLimitRejectsBeforeProviderCall() {
        ChatModel chatModel = mock(ChatModel.class);
        MockResponseProvider mockResponseProvider = mock(MockResponseProvider.class);
        AiCallRecorder recorder = mock(AiCallRecorder.class);
        AiRateLimitService rateLimitService = mock(AiRateLimitService.class);
        AiClient client = new AiClient(chatModel, mockResponseProvider, recorder, rateLimitService,
                "openai", "https://api.deepseek.com", "deepseek-v4-flash");
        AiCallContext context = new AiCallContext(
                AiFeatureType.INTERROGATION,
                "npc_interrogation_v1",
                10L,
                20L,
                30L,
                "NPC_SECRETARY"
        );

        when(rateLimitService.checkAndConsume(context))
                .thenThrow(new AiException(AiErrorCode.AI_DAILY_RATE_LIMIT_EXCEEDED));

        assertThatThrownBy(() -> client.chatWithMetadata(
                "system", "user", AiRequestParams.interrogation(0.3, 150), context))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_DAILY_RATE_LIMIT_EXCEEDED));

        verify(recorder).recordQuotaBlock(any(AiCallContext.class), anyString(), anyLong());
        verify(recorder, never()).record(
                any(AiCallContext.class),
                anyString(),
                anyString(),
                anyLong(),
                anyBoolean(),
                any(),
                anyBoolean(),
                any()
        );
        verify(chatModel, never()).call(any(Prompt.class));
    }
}
