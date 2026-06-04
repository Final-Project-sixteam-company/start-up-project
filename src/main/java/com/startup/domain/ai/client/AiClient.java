package com.startup.domain.ai.client;

import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.Generation;
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

@Slf4j
@Component
public class AiClient {

    private final ChatModel chatModel;
    private final MockResponseProvider mockResponseProvider;
    private final boolean mockMode;
    private final boolean debugResponseLogEnabled;

    public AiClient(@Autowired(required = false) ChatModel chatModel,
                    MockResponseProvider mockResponseProvider,
                    @Value("${spring.ai.model.chat:none}") String chatModelType,
                    @Value("${caselab.ai.debug-response-log-enabled:false}") boolean debugResponseLogEnabled) {
        this.chatModel = chatModel;
        this.mockResponseProvider = mockResponseProvider;
        this.mockMode = "none".equalsIgnoreCase(chatModelType);
        this.debugResponseLogEnabled = debugResponseLogEnabled;
    }

    public String chat(String systemPrompt, String userPrompt, AiRequestParams params) {
        if (chatModel == null) {
            throw new AiException(AiErrorCode.AI_SERVICE_UNAVAILABLE,
                    "ChatModel not configured. Set SPRING_AI_MODEL_CHAT in .env");
        }

        long startTime = System.currentTimeMillis();
        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .temperature(params.getTemperature())
                    .maxTokens(params.getMaxTokens())
                    .build();

            Prompt prompt = new Prompt(
                    List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                    options
            );

            ChatResponse response = chatModel.call(prompt);
            long latency = System.currentTimeMillis() - startTime;
            log.info("AI 호출 완료: latency={}ms", latency);

            if (response == null || response.getResult() == null) {
                throw new AiException(AiErrorCode.AI_INVALID_RESPONSE);
            }

            AssistantMessage output = response.getResult().getOutput();
            if (output == null) {
                logChatResponse(response, null);
                throw new AiException(AiErrorCode.AI_INVALID_RESPONSE);
            }

            String text = output.getText();
            logChatResponse(response, output);
            return text;
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("AI 호출 실패: latency={}ms", latency, e);
            throw new AiException(AiErrorCode.AI_REQUEST_FAILED, e);
        }
    }

    public String chatOrMock(String systemPrompt, String userPrompt, AiRequestParams params,
                             Long suspectId, boolean hasPresented) {
        if (mockMode) {
            log.debug("Mock 모드: suspectId={}, hasPresented={}", suspectId, hasPresented);
            return mockResponseProvider.getResponse(suspectId, hasPresented);
        }
        return chat(systemPrompt, userPrompt, params);
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public String getModelName() {
        return mockMode ? "MOCK" : "openai";
    }

    private void logChatResponse(ChatResponse response, AssistantMessage output) {
        if (!debugResponseLogEnabled) {
            return;
        }

        int resultCount = response.getResults() == null ? 0 : response.getResults().size();
        Generation result = response.getResult();
        ChatGenerationMetadata generationMetadata = result == null ? null : result.getMetadata();
        ChatResponseMetadata responseMetadata = response.getMetadata();
        Usage usage = responseMetadata == null ? null : responseMetadata.getUsage();
        String text = output == null ? null : output.getText();

        log.info(
                "AI ChatResponse debug: resultCount={}, finishReason={}, model={}, "
                        + "promptTokens={}, completionTokens={}, totalTokens={}, "
                        + "generationMetadataKeys={}, outputClass={}, outputMetadataKeys={}, "
                        + "textPresent={}, textLength={}",
                resultCount,
                generationMetadata == null ? null : generationMetadata.getFinishReason(),
                responseMetadata == null ? null : responseMetadata.getModel(),
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens(),
                generationMetadata == null ? null : generationMetadata.keySet(),
                output == null ? null : output.getClass().getName(),
                output == null ? null : output.getMetadata().keySet(),
                text != null && !text.isBlank(),
                text == null ? null : text.length()
        );
    }
}
