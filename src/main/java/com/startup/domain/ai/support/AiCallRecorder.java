package com.startup.domain.ai.support;

import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.client.AiTokenUsage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AiCallRecorder {

    private static final String UNKNOWN = "unknown";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final AiCallLogWriter aiCallLogWriter;

    public AiCallRecorder(ObjectProvider<MeterRegistry> meterRegistryProvider,
                          AiCallLogWriter aiCallLogWriter) {
        this.meterRegistryProvider = meterRegistryProvider;
        this.aiCallLogWriter = aiCallLogWriter;
    }

    public void record(AiCallContext context,
                       String provider,
                       String model,
                       long latencyMs,
                       boolean success,
                       String errorCode,
                       boolean fallbackUsed,
                       AiTokenUsage usage) {
        AiCallContext safeContext = context == null ? AiCallContext.unknown() : context;
        String safeProvider = safe(provider);
        String safeModel = safe(model);
        String safePromptVersion = safe(safeContext.promptVersion());
        String safeErrorCode = errorCode == null ? "none" : safe(errorCode);

        log.info(
                "AI_CALL featureType={} provider={} model={} promptVersion={} scenarioId={} sessionId={} suspectId={} npcCode={} latencyMs={} success={} errorCode={} fallbackUsed={} promptTokens={} completionTokens={} totalTokens={}",
                safeContext.featureType(),
                safeProvider,
                safeModel,
                safePromptVersion,
                safeContext.scenarioId(),
                safeContext.sessionId(),
                safeContext.suspectId(),
                safe(safeContext.npcCode()),
                latencyMs,
                success,
                safeErrorCode,
                fallbackUsed,
                usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens(),
                usage == null ? null : usage.totalTokens()
        );

        recordMetrics(safeContext, safeProvider, safeModel, safePromptVersion,
                latencyMs, success, safeErrorCode, fallbackUsed, usage);
        aiCallLogWriter.write(safeContext, safeProvider, safeModel, safePromptVersion,
                latencyMs, success, safeErrorCode, fallbackUsed, usage);
    }

    public void recordQuotaBlock(AiCallContext context, String errorCode, long latencyMs) {
        AiCallContext safeContext = context == null ? AiCallContext.unknown() : context;
        String safePromptVersion = safe(safeContext.promptVersion());
        String safeErrorCode = errorCode == null ? "unknown" : safe(errorCode);

        log.info(
                "AI_QUOTA_BLOCK featureType={} promptVersion={} scenarioId={} sessionId={} suspectId={} npcCode={} latencyMs={} errorCode={}",
                safeContext.featureType(),
                safePromptVersion,
                safeContext.scenarioId(),
                safeContext.sessionId(),
                safeContext.suspectId(),
                safe(safeContext.npcCode()),
                Math.max(0, latencyMs),
                safeErrorCode
        );

        recordQuotaMetric(safeContext, safePromptVersion, Math.max(0, latencyMs), safeErrorCode);
    }

    private void recordMetrics(AiCallContext context,
                               String provider,
                               String model,
                               String promptVersion,
                               long latencyMs,
                               boolean success,
                               String errorCode,
                               boolean fallbackUsed,
                               AiTokenUsage usage) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }

        try {
            Tags baseTags = Tags.of(
                    "feature_type", context.featureType().name(),
                    "provider", provider,
                    "model", model,
                    "prompt_version", promptVersion,
                    "success", String.valueOf(success),
                    "fallback_used", String.valueOf(fallbackUsed)
            );

            Counter.builder("ai.requests")
                    .description("Total AI feature calls, including mock and fallback paths.")
                    .tags(baseTags)
                    .register(registry)
                    .increment();

            Timer.builder("ai.latency")
                    .description("AI feature call latency.")
                    .tags(baseTags)
                    .register(registry)
                    .record(Math.max(0, latencyMs), TimeUnit.MILLISECONDS);

            if (!success) {
                Counter.builder("ai.failures")
                        .description("Total failed AI provider calls.")
                        .tags(baseTags.and("error_code", errorCode))
                        .register(registry)
                        .increment();
            }

            if (fallbackUsed) {
                Counter.builder("ai.fallbacks")
                        .description("Total AI fallback responses used.")
                        .tags(baseTags.and("error_code", errorCode))
                        .register(registry)
                        .increment();
            }

            recordTokenMetric(registry, baseTags, "prompt", usage == null ? null : usage.promptTokens());
            recordTokenMetric(registry, baseTags, "completion", usage == null ? null : usage.completionTokens());
            recordTokenMetric(registry, baseTags, "total", usage == null ? null : usage.totalTokens());
        } catch (Exception e) {
            log.debug("AI_CALL metric recording skipped: {}", e.getMessage());
        }
    }

    private void recordQuotaMetric(AiCallContext context,
                                   String promptVersion,
                                   long latencyMs,
                                   String errorCode) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }

        try {
            Tags tags = Tags.of(
                    "feature_type", context.featureType().name(),
                    "prompt_version", promptVersion,
                    "error_code", errorCode
            );

            Counter.builder("ai.quota.blocks")
                    .description("Total AI requests blocked before provider calls by quota/rate-limit policy.")
                    .tags(tags)
                    .register(registry)
                    .increment();

            Timer.builder("ai.quota.latency")
                    .description("AI quota check latency for blocked requests.")
                    .tags(tags)
                    .register(registry)
                    .record(Math.max(0, latencyMs), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("AI_QUOTA_BLOCK metric recording skipped: {}", e.getMessage());
        }
    }

    private void recordTokenMetric(MeterRegistry registry, Tags baseTags, String tokenType, Integer amount) {
        if (amount == null || amount <= 0) {
            return;
        }

        Counter.builder("ai.tokens")
                .description("Total AI token usage when provider metadata exposes it.")
                .tags(baseTags.and("token_type", tokenType))
                .register(registry)
                .increment(amount);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value.replaceAll("[\\r\\n\\t]+", " ").strip();
    }
}
