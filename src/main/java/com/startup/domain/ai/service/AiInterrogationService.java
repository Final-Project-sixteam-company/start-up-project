package com.startup.domain.ai.service;

import com.startup.common.auth.MockUserProvider;
import com.startup.domain.ai.client.AiClient;
import com.startup.domain.ai.client.AiRequestParams;
import com.startup.domain.ai.client.MockResponseProvider;
import com.startup.domain.ai.dto.InterrogationCompletedEvent;
import com.startup.domain.ai.dto.InterrogationContext;
import com.startup.domain.ai.dto.InterrogationRequest;
import com.startup.domain.ai.dto.InterrogationResponse;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.ai.support.InterrogationContextLoader;
import com.startup.domain.ai.support.InterrogationLogWriter;
import com.startup.domain.play.service.InterrogationEvidenceUnlockService;
import com.startup.domain.play.service.TimeEvidenceUnlockSyncer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInterrogationService {

    private final InterrogationContextLoader contextLoader;
    private final AiPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final MockResponseProvider mockResponseProvider;
    private final InterrogationLogWriter logWriter;
    private final InterrogationLogRepository interrogationLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TimeEvidenceUnlockSyncer timeEvidenceUnlockSyncer;
    private final InterrogationEvidenceUnlockService interrogationEvidenceUnlockService;
    private final MockUserProvider mockUserProvider;

    @Value("${caselab.ai.interrogation.temperature:0.3}")
    private double temperature;

    @Value("${caselab.ai.interrogation.max-tokens:150}")
    private int maxTokens;

    public InterrogationResponse interrogate(Long sessionId, InterrogationRequest request) {
        // readOnly 트랜잭션 시작 전에 시간 해금 동기화 (REPEATABLE READ 대응)
        timeEvidenceUnlockSyncer.sync(sessionId, mockUserProvider.currentUserId());

        // 1. 데이터 조회 (readOnly 트랜잭션 — InterrogationContextLoader)
        InterrogationContext context = contextLoader.load(
                sessionId, request.suspectId(), request.presentedEvidenceId());

        // 2. AI 호출 (트랜잭션 밖)
        AiResult result = callAi(context, request);

        // 3. 로그 저장 (쓰기 트랜잭션 — InterrogationLogWriter)
        InterrogationLog savedLog = logWriter.save(
                sessionId,
                request.suspectId(),
                request.presentedEvidenceId(),
                request.questionType(),
                request.question(),
                result.answer(),
                result.modelName()
        );

        // 4. 이벤트 발행 (향후 비동기/분석용 확장 대비 보존)
        publishEvent(sessionId, request);

        // 5. 증거 제시 기반 해금 (A안: domain/play 서비스 동기 호출 → 새로 해금된 증거 diff)
        List<InterrogationResponse.UnlockedEvidenceDto> unlockedEvidences =
                resolveUnlockedEvidences(sessionId, request);

        return new InterrogationResponse(
                savedLog.getId(),
                context.suspect().id(),
                context.suspect().name(),
                request.question(),
                result.answer(),
                unlockedEvidences,
                savedLog.getCreatedAt()
        );
    }

    private List<InterrogationResponse.UnlockedEvidenceDto> resolveUnlockedEvidences(
            Long sessionId, InterrogationRequest request) {
        // 이중 방어: @AssertTrue가 우회되더라도 증거 제시 심문 + 제시 증거가 있을 때만 해금을 시도한다.
        if (request.questionType() != QuestionType.EVIDENCE_PRESENTED
                || request.presentedEvidenceId() == null) {
            return List.of();
        }
        // 세션 소유자/PLAYING 검증은 contextLoader.load 단계에서 이미 끝났다.
        return interrogationEvidenceUnlockService
                .unlockByPresentedEvidence(
                        sessionId, request.suspectId(), request.presentedEvidenceId(),
                        mockUserProvider.currentUserId())
                .stream()
                .map(u -> new InterrogationResponse.UnlockedEvidenceDto(u.evidenceId(), u.title()))
                .toList();
    }

    private AiResult callAi(InterrogationContext context, InterrogationRequest request) {
        boolean hasPresented = request.presentedEvidenceId() != null;

        if (aiClient.isMockMode()) {
            String answer = aiClient.chatOrMock(null, null, null, request.suspectId(), hasPresented);
            return new AiResult(answer, "MOCK");
        }

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(
                context.suspect(),
                context.revealedEvidences(),
                context.presentedEvidence(),
                context.policy(),
                context.history(),
                request.question(),
                request.questionType()
        );

        AiRequestParams params = AiRequestParams.interrogation(temperature, maxTokens);

        try {
            String answer = aiClient.chat(systemPrompt, userPrompt, params);
            return new AiResult(answer, aiClient.getModelName());
        } catch (AiException e) {
            log.warn("AI 호출 실패, Fallback 응답 반환: {}", e.getMessage());
            return new AiResult(mockResponseProvider.getFallbackResponse(), "FALLBACK");
        }
    }

    private void publishEvent(Long sessionId, InterrogationRequest request) {
        int count = interrogationLogRepository.countByPlaySessionIdAndSuspectId(
                sessionId, request.suspectId());

        eventPublisher.publishEvent(new InterrogationCompletedEvent(
                sessionId,
                request.suspectId(),
                request.presentedEvidenceId(),
                count
        ));
    }

    private record AiResult(String answer, String modelName) {}
}
