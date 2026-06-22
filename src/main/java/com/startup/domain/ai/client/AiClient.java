package com.startup.domain.ai.client;

import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.dto.AiQuotaStatus;
import com.startup.domain.ai.support.AiCallRecorder;
import com.startup.domain.ai.support.AiRateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiClient {

    private final ChatModel chatModel;
    private final MockResponseProvider mockResponseProvider;
    private final AiCallRecorder aiCallRecorder;
    private final AiRateLimitService aiRateLimitService;
    private final boolean mockMode;
    private final String baseUrl;
    private final String configuredModel;

    public AiClient(@Autowired(required = false) ChatModel chatModel,
                    MockResponseProvider mockResponseProvider,
                    AiCallRecorder aiCallRecorder,
                    AiRateLimitService aiRateLimitService,
                    @Value("${spring.ai.model.chat:none}") String chatModelType,
                    @Value("${spring.ai.openai.base-url:}") String baseUrl,
                    @Value("${spring.ai.openai.chat.options.model:unknown}") String configuredModel) {
        this.chatModel = chatModel;
        this.mockResponseProvider = mockResponseProvider;
        this.aiCallRecorder = aiCallRecorder;
        this.aiRateLimitService = aiRateLimitService;
        this.mockMode = "none".equalsIgnoreCase(chatModelType);
        this.baseUrl = baseUrl;
        this.configuredModel = configuredModel;
    }

    public String chat(String systemPrompt, String userPrompt, AiRequestParams params) {
        return chatWithMetadata(systemPrompt, userPrompt, params, AiCallContext.unknown()).text();
    }

    public AiCallResult chatWithMetadata(String systemPrompt, String userPrompt,
                                         AiRequestParams params, AiCallContext context) {
        long startTime = System.currentTimeMillis();
        if (chatModel == null) {
            long latency = System.currentTimeMillis() - startTime;
            aiCallRecorder.record(context, providerName(), getModelName(), latency,
                    false, AiErrorCode.AI_SERVICE_UNAVAILABLE.getCode(), false, null);
            throw new AiException(AiErrorCode.AI_SERVICE_UNAVAILABLE,
                    "ChatModel not configured. Set SPRING_AI_MODEL_CHAT in .env");
        }

        AiQuotaStatus quotaStatus;
        try {
            quotaStatus = aiRateLimitService.checkAndConsume(context);
        } catch (AiException e) {
            long latency = System.currentTimeMillis() - startTime;
            aiCallRecorder.recordQuotaBlock(context, e.getErrorCode().getCode(), latency);
            throw e;
        }

        try {
            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                    .temperature(params.getTemperature())
                    .maxTokens(params.getMaxTokens());
            if (shouldDisableThinking()) {
                // DeepSeek V4 모델은 기본 thinking ON이라 짧은 NPC 답변에서 CoT가 토큰을 잠식한다.
                // OpenAI-compat 경로로 thinking:disabled를 실어 비-thinking으로 고정한다(다른 provider 미적용).
                optionsBuilder.extraBody(Map.of("thinking", Map.of("type", "disabled")));
            }
            OpenAiChatOptions options = optionsBuilder.build();

            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                    options
            );

            ChatResponse response = chatModel.call(prompt);

            if (response == null || response.getResult() == null) {
                throw new AiException(AiErrorCode.AI_INVALID_RESPONSE);
            }

            String text = response.getResult().getOutput().getText();
            long latency = System.currentTimeMillis() - startTime;
            String modelName = resolveModelName(response);
            AiTokenUsage usage = extractUsage(response);
            aiCallRecorder.record(context, providerName(), modelName, latency, true, null, false, usage);

            return new AiCallResult(text, modelName, latency, usage, false, quotaStatus);
        } catch (AiException e) {
            long latency = System.currentTimeMillis() - startTime;
            aiCallRecorder.record(context, providerName(), getModelName(), latency,
                    false, e.getErrorCode().getCode(), false, null);
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            aiCallRecorder.record(context, providerName(), getModelName(), latency,
                    false, AiErrorCode.AI_REQUEST_FAILED.getCode(), false, null);
            log.error("AI provider call failed: latency={}ms", latency, e);
            throw new AiException(AiErrorCode.AI_REQUEST_FAILED, e);
        }
    }

    public String chatOrMock(String systemPrompt, String userPrompt, AiRequestParams params,
                             Long suspectId, boolean hasPresented) {
        return chatOrMockWithMetadata(systemPrompt, userPrompt, params, AiCallContext.unknown(),
                suspectId, hasPresented).text();
    }

    public AiCallResult chatOrMockWithMetadata(String systemPrompt, String userPrompt, AiRequestParams params,
                                               AiCallContext context, Long suspectId, boolean hasPresented) {
        if (mockMode) {
            log.debug("Mock 모드: suspectId={}, hasPresented={}", suspectId, hasPresented);
            String response = mockResponseProvider.getResponse(suspectId, hasPresented);
            aiCallRecorder.record(context, "mock", "MOCK", 0L, true, null, false, null);
            return new AiCallResult(response, "MOCK", 0L, null, false);
        }
        return chatWithMetadata(systemPrompt, userPrompt, params, context);
    }

    public void recordMock(AiCallContext context) {
        aiCallRecorder.record(context, "mock", "MOCK", 0L, true, null, false, null);
    }

    public void recordFallback(AiCallContext context, String errorCode) {
        recordFallback(context, errorCode, 0L);
    }

    public void recordFallback(AiCallContext context, String errorCode, long latencyMs) {
        aiCallRecorder.record(context, "fallback", "FALLBACK", Math.max(0L, latencyMs),
                true, errorCode, true, null);
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public String getModelName() {
        return mockMode ? "MOCK" : configuredModel;
    }

    public String getProviderName() {
        return providerName();
    }

    private String providerName() {
        if (mockMode) {
            return "mock";
        }
        String normalized = baseUrl == null ? "" : baseUrl.toLowerCase();
        if (normalized.contains("deepseek")) {
            return "deepseek";
        }
        if (normalized.contains("openai")) {
            return "openai";
        }
        return "openai-compatible";
    }

    private boolean shouldDisableThinking() {
        // thinking 제어는 모델 종속이다. 다른 provider에는 deepseek-v4* 모델이 없어 오염되지 않는다.
        String model = configuredModel == null ? "" : configuredModel.toLowerCase();
        return model.startsWith("deepseek-v4");
    }

    private String resolveModelName(ChatResponse response) {
        if (response != null
                && response.getMetadata() != null
                && response.getMetadata().getModel() != null
                && !response.getMetadata().getModel().isBlank()) {
            return response.getMetadata().getModel();
        }
        return getModelName();
    }

    private AiTokenUsage extractUsage(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return null;
        }

        Usage usage = response.getMetadata().getUsage();
        if (usage == null) {
            return null;
        }

        return new AiTokenUsage(
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens()
        );
    }
}
