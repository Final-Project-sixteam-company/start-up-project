package com.startup.domain.ai.client;

import com.startup.domain.ai.enums.AiFeatureType;
import com.startup.domain.ai.support.AiCallRecorder;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiClientTest {

    @Test
    void recordFallback_preservesProvidedLatency() {
        MockResponseProvider mockResponseProvider = mock(MockResponseProvider.class);
        AiCallRecorder recorder = mock(AiCallRecorder.class);
        AiClient client = new AiClient(null, mockResponseProvider, recorder,
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
        AiClient client = new AiClient(null, mockResponseProvider, recorder,
                "none", "", "MOCK");
        AiCallContext context = AiCallContext.unknown();

        client.recordFallback(context, "AI001", -1L);

        verify(recorder).record(context, "fallback", "FALLBACK",
                0L, true, "AI001", true, null);
    }
}
