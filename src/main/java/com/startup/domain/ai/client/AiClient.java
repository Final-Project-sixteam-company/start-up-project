package com.startup.domain.ai.client;

import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import lombok.extern.slf4j.Slf4j;
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

    public AiClient(@Autowired(required = false) ChatModel chatModel,
                    MockResponseProvider mockResponseProvider,
                    @Value("${spring.ai.model.chat:none}") String chatModelType) {
        this.chatModel = chatModel;
        this.mockResponseProvider = mockResponseProvider;
        this.mockMode = "none".equalsIgnoreCase(chatModelType);
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

            return response.getResult().getOutput().getText();
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
}
