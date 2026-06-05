package com.startup.domain.ai.support;

import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.client.AiTokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class AiCallLogWriter {

    private static final String INSERT_SQL = """
            INSERT INTO ai_call_logs (
                feature_type,
                provider,
                model,
                prompt_version,
                scenario_id,
                play_session_id,
                suspect_id,
                npc_code,
                latency_ms,
                success,
                error_code,
                fallback_used,
                prompt_tokens,
                completion_tokens,
                total_tokens,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final boolean enabled;
    private final AtomicBoolean failureWarned = new AtomicBoolean(false);

    public AiCallLogWriter(ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                           @Value("${caselab.ai.llmops.db-logging-enabled:false}") boolean enabled) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.enabled = enabled;
    }

    public void write(AiCallContext context,
                      String provider,
                      String model,
                      String promptVersion,
                      long latencyMs,
                      boolean success,
                      String errorCode,
                      boolean fallbackUsed,
                      AiTokenUsage usage) {
        if (!enabled) {
            return;
        }

        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            warnOnce("AI_CALL DB logging is enabled but JdbcTemplate is unavailable.");
            return;
        }

        try {
            jdbcTemplate.update(
                    INSERT_SQL,
                    context.featureType().name(),
                    provider,
                    model,
                    promptVersion,
                    context.scenarioId(),
                    context.sessionId(),
                    context.suspectId(),
                    context.npcCode(),
                    latencyMs,
                    success,
                    errorCode,
                    fallbackUsed,
                    usage == null ? null : usage.promptTokens(),
                    usage == null ? null : usage.completionTokens(),
                    usage == null ? null : usage.totalTokens()
            );
        } catch (Exception e) {
            warnOnce("AI_CALL DB logging failed. Disable AI_LLMOPS_DB_LOGGING_ENABLED or create ai_call_logs table first: "
                    + e.getMessage());
        }
    }

    private void warnOnce(String message) {
        if (failureWarned.compareAndSet(false, true)) {
            log.warn(message);
        } else {
            log.debug(message);
        }
    }
}
