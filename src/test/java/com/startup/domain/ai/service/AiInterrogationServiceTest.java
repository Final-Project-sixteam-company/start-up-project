package com.startup.domain.ai.service;

import com.startup.common.auth.MockUserProvider;
import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.client.AiCallResult;
import com.startup.domain.ai.client.AiClient;
import com.startup.domain.ai.client.AiRequestParams;
import com.startup.domain.ai.client.MockResponseProvider;
import com.startup.domain.ai.dto.InterrogationContext;
import com.startup.domain.ai.dto.InterrogationRequest;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.enums.AiFeatureType;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.ai.support.AiPromptContextLogger;
import com.startup.domain.ai.support.InterrogationContextLoader;
import com.startup.domain.ai.support.InterrogationLogWriter;
import com.startup.domain.play.service.InterrogationEvidenceUnlockService;
import com.startup.domain.play.service.TimeEvidenceUnlockSyncer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiInterrogationServiceTest {

    @Test
    void interrogate_populatesLlmOpsContextWithScenarioIdAndNpcCode() {
        InterrogationContextLoader contextLoader = mock(InterrogationContextLoader.class);
        AiPromptBuilder promptBuilder = mock(AiPromptBuilder.class);
        AiClient aiClient = mock(AiClient.class);
        MockResponseProvider mockResponseProvider = mock(MockResponseProvider.class);
        InterrogationLogWriter logWriter = mock(InterrogationLogWriter.class);
        InterrogationLogRepository interrogationLogRepository = mock(InterrogationLogRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        TimeEvidenceUnlockSyncer timeEvidenceUnlockSyncer = mock(TimeEvidenceUnlockSyncer.class);
        InterrogationEvidenceUnlockService interrogationEvidenceUnlockService =
                mock(InterrogationEvidenceUnlockService.class);
        MockUserProvider mockUserProvider = mock(MockUserProvider.class);
        AiPromptContextLogger promptContextLogger = mock(AiPromptContextLogger.class);
        AiInterrogationService service = new AiInterrogationService(
                contextLoader,
                promptBuilder,
                aiClient,
                mockResponseProvider,
                logWriter,
                interrogationLogRepository,
                eventPublisher,
                timeEvidenceUnlockSyncer,
                interrogationEvidenceUnlockService,
                mockUserProvider,
                promptContextLogger
        );
        Long scenarioId = 10L;
        Long sessionId = 20L;
        Long suspectId = 30L;
        String suspectCode = "SUSPECT_SECRETARY";
        InterrogationRequest request = new InterrogationRequest(
                suspectId, QuestionType.FREE, "어디에 있었습니까?", null);

        when(mockUserProvider.currentUserId()).thenReturn(1L);
        when(contextLoader.load(sessionId, suspectId, null)).thenReturn(new InterrogationContext(
                scenarioId,
                new SuspectProfile(
                        suspectId,
                        suspectCode,
                        "문하연",
                        "비서실장",
                        "측근",
                        "공개 프로필",
                        "공개 진술",
                        "알리바이"
                ),
                List.of(),
                null,
                ResponsePolicyResult.hardcodedFallback(),
                List.of()
        ));
        when(aiClient.isMockMode()).thenReturn(true);
        when(aiClient.chatOrMockWithMetadata(
                isNull(), isNull(), isNull(), any(AiCallContext.class), anyLong(), anyBoolean()))
                .thenReturn(new AiCallResult("답변", "MOCK", 0L, null, false));
        when(logWriter.save(
                anyLong(), anyLong(), isNull(), any(QuestionType.class), any(), any(), any()))
                .thenReturn(InterrogationLog.builder()
                        .playSessionId(sessionId)
                        .suspectId(suspectId)
                        .questionType(QuestionType.FREE)
                        .question(request.question())
                        .answer("답변")
                        .aiModel("MOCK")
                        .build());
        when(interrogationLogRepository.countByPlaySessionIdAndSuspectId(sessionId, suspectId))
                .thenReturn(1);

        service.interrogate(sessionId, request);

        ArgumentCaptor<AiCallContext> contextCaptor = ArgumentCaptor.forClass(AiCallContext.class);
        verify(aiClient).chatOrMockWithMetadata(
                isNull(), isNull(), isNull(), contextCaptor.capture(), anyLong(), anyBoolean());
        AiCallContext aiCallContext = contextCaptor.getValue();
        assertThat(aiCallContext.featureType()).isEqualTo(AiFeatureType.INTERROGATION);
        assertThat(aiCallContext.promptVersion()).isEqualTo("npc_interrogation_v1");
        assertThat(aiCallContext.scenarioId()).isEqualTo(scenarioId);
        assertThat(aiCallContext.sessionId()).isEqualTo(sessionId);
        assertThat(aiCallContext.suspectId()).isEqualTo(suspectId);
        assertThat(aiCallContext.npcCode()).isEqualTo(suspectCode);
    }

    @Test
    void interrogate_recordsPromptContextBeforeRealAiCall() {
        InterrogationContextLoader contextLoader = mock(InterrogationContextLoader.class);
        AiPromptBuilder promptBuilder = mock(AiPromptBuilder.class);
        AiClient aiClient = mock(AiClient.class);
        MockResponseProvider mockResponseProvider = mock(MockResponseProvider.class);
        InterrogationLogWriter logWriter = mock(InterrogationLogWriter.class);
        InterrogationLogRepository interrogationLogRepository = mock(InterrogationLogRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        TimeEvidenceUnlockSyncer timeEvidenceUnlockSyncer = mock(TimeEvidenceUnlockSyncer.class);
        InterrogationEvidenceUnlockService interrogationEvidenceUnlockService =
                mock(InterrogationEvidenceUnlockService.class);
        MockUserProvider mockUserProvider = mock(MockUserProvider.class);
        AiPromptContextLogger promptContextLogger = mock(AiPromptContextLogger.class);
        AiInterrogationService service = new AiInterrogationService(
                contextLoader,
                promptBuilder,
                aiClient,
                mockResponseProvider,
                logWriter,
                interrogationLogRepository,
                eventPublisher,
                timeEvidenceUnlockSyncer,
                interrogationEvidenceUnlockService,
                mockUserProvider,
                promptContextLogger
        );
        Long scenarioId = 10L;
        Long sessionId = 20L;
        Long suspectId = 30L;
        InterrogationRequest request = new InterrogationRequest(
                suspectId, QuestionType.FREE, "마지막으로 피해자를 본 시간은?", null);
        SuspectProfile suspect = new SuspectProfile(
                suspectId,
                "NPC_SECRETARY",
                "문하연",
                "비서실장",
                "측근",
                "공개 프로필",
                "공개 진술",
                "알리바이"
        );
        InterrogationContext context = new InterrogationContext(
                scenarioId,
                suspect,
                List.of(),
                null,
                ResponsePolicyResult.hardcodedFallback(),
                List.of()
        );

        when(mockUserProvider.currentUserId()).thenReturn(1L);
        when(contextLoader.load(sessionId, suspectId, null)).thenReturn(context);
        when(aiClient.isMockMode()).thenReturn(false);
        when(aiClient.getProviderName()).thenReturn("deepseek");
        when(aiClient.getModelName()).thenReturn("deepseek-v4-flash");
        when(promptBuilder.buildSystemPrompt()).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(
                eq(suspect), eq(List.of()), isNull(), eq(context.policy()), eq(List.of()),
                eq(request.question()), eq(request.questionType())))
                .thenReturn("user prompt");
        when(promptBuilder.interrogationTemplateHash(request.questionType())).thenReturn("abc123def456");
        when(aiClient.chatWithMetadata(
                anyString(), anyString(), any(AiRequestParams.class), any(AiCallContext.class)))
                .thenReturn(new AiCallResult("답변", "deepseek-v4-flash", 100L, null, false));
        when(logWriter.save(
                anyLong(), anyLong(), isNull(), any(QuestionType.class), any(), any(), any()))
                .thenReturn(InterrogationLog.builder()
                        .playSessionId(sessionId)
                        .suspectId(suspectId)
                        .questionType(QuestionType.FREE)
                        .question(request.question())
                        .answer("답변")
                        .aiModel("deepseek-v4-flash")
                        .build());
        when(interrogationLogRepository.countByPlaySessionIdAndSuspectId(sessionId, suspectId))
                .thenReturn(1);

        service.interrogate(sessionId, request);

        verify(promptContextLogger).recordInterrogation(
                any(AiCallContext.class),
                eq("deepseek"),
                eq("deepseek-v4-flash"),
                eq("system prompt"),
                eq("user prompt"),
                eq(suspect),
                eq(List.of()),
                isNull(),
                eq(context.policy()),
                eq(List.of()),
                eq(request.question()),
                eq(request.questionType()),
                eq("abc123def456")
        );
    }
}
