package com.startup.domain.ai.support;

import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.client.AiTokenUsage;
import com.startup.domain.ai.enums.AiFeatureType;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiCallRecorderTest {

    @Test
    void record_success_recordsRequestLatencyTokenMetricsAndDbLog() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiCallLogWriter writer = mock(AiCallLogWriter.class);
        AiCallRecorder recorder = new AiCallRecorder(meterRegistryProvider(registry), writer);

        recorder.record(context(), "deepseek", "deepseek-chat", 1250L,
                true, null, false, new AiTokenUsage(11, 22, 33));

        assertThat(registry.get("ai.requests")
                .tag("feature_type", "INTERROGATION")
                .tag("provider", "deepseek")
                .tag("model", "deepseek-chat")
                .tag("prompt_version", "npc_interrogation_v1")
                .tag("success", "true")
                .tag("fallback_used", "false")
                .counter()
                .count()).isEqualTo(1.0);

        assertThat(registry.get("ai.latency").timer().count()).isEqualTo(1);
        assertThat(registry.get("ai.latency").timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(1250.0);

        assertThat(registry.get("ai.tokens").tag("token_type", "prompt").counter().count()).isEqualTo(11.0);
        assertThat(registry.get("ai.tokens").tag("token_type", "completion").counter().count()).isEqualTo(22.0);
        assertThat(registry.get("ai.tokens").tag("token_type", "total").counter().count()).isEqualTo(33.0);

        assertNoHighCardinalityTags(registry);
        verify(writer).write(any(AiCallContext.class), anyString(), anyString(), anyString(),
                anyLong(), anyBoolean(), anyString(), anyBoolean(), any(AiTokenUsage.class));
    }

    @Test
    void record_failureFallback_recordsFailureAndFallbackMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AiCallLogWriter writer = mock(AiCallLogWriter.class);
        AiCallRecorder recorder = new AiCallRecorder(meterRegistryProvider(registry), writer);

        recorder.record(context(), "fallback", "FALLBACK", 0L,
                false, "AI001", true, null);

        assertThat(registry.get("ai.failures").tag("error_code", "AI001").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("ai.fallbacks").tag("error_code", "AI001").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("ai.tokens").meters()).isEmpty();

        assertNoHighCardinalityTags(registry);
    }

    private AiCallContext context() {
        return new AiCallContext(
                AiFeatureType.INTERROGATION,
                "npc_interrogation_v1",
                10L,
                20L,
                30L,
                "NPC_SECRETARY"
        );
    }

    private ObjectProvider<MeterRegistry> meterRegistryProvider(MeterRegistry registry) {
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        return provider;
    }

    private void assertNoHighCardinalityTags(MeterRegistry registry) {
        Set<String> forbiddenTagKeys = Set.of(
                "scenarioId",
                "scenario_id",
                "sessionId",
                "session_id",
                "suspectId",
                "suspect_id",
                "npcCode",
                "npc_code",
                "requestId",
                "request_id"
        );

        for (Meter meter : registry.getMeters()) {
            assertThat(meter.getId().getTags())
                    .extracting(tag -> tag.getKey())
                    .doesNotContainAnyElementsOf(forbiddenTagKeys);
        }
    }
}
