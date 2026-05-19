package com.startup.domain.ai.service;

import com.startup.domain.ai.client.AiClient;
import com.startup.domain.ai.client.AiRequestParams;
import com.startup.domain.ai.client.MockResponseProvider;
import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.InterrogationCompletedEvent;
import com.startup.domain.ai.dto.InterrogationRequest;
import com.startup.domain.ai.dto.InterrogationResponse;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.ai.support.EvidenceReader;
import com.startup.domain.ai.support.InterrogationHistoryProvider;
import com.startup.domain.ai.support.InterrogationLogWriter;
import com.startup.domain.ai.support.PlaySessionReader;
import com.startup.domain.ai.support.ResponsePolicyResolver;
import com.startup.domain.ai.support.SuspectReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInterrogationService {

    private final PlaySessionReader playSessionReader;
    private final SuspectReader suspectReader;
    private final EvidenceReader evidenceReader;
    private final ResponsePolicyResolver policyResolver;
    private final InterrogationHistoryProvider historyProvider;
    private final AiPromptBuilder promptBuilder;
    private final AiClient aiClient;
    private final MockResponseProvider mockResponseProvider;
    private final InterrogationLogWriter logWriter;
    private final InterrogationLogRepository interrogationLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${caselab.ai.interrogation.max-history-turns:5}")
    private int maxHistoryTurns;

    @Value("${caselab.ai.interrogation.temperature:0.3}")
    private double temperature;

    @Value("${caselab.ai.interrogation.max-tokens:150}")
    private int maxTokens;

    public InterrogationResponse interrogate(Long sessionId, InterrogationRequest request) {
        // 1. 데이터 조회 (readOnly 트랜잭션)
        InterrogationContext context = loadContext(sessionId, request);

        // 2. AI 호출 (트랜잭션 밖)
        AiResult result = callAi(context, request);

        // 3. 로그 저장 (새 트랜잭션 — InterrogationLogWriter)
        logWriter.save(
                sessionId,
                request.suspectId(),
                request.presentedEvidenceId(),
                request.questionType(),
                request.question(),
                result.answer(),
                result.modelName()
        );

        // 4. 이벤트 발행
        publishEvent(sessionId, request);

        return new InterrogationResponse(
                context.suspect().id(),
                context.suspect().name(),
                result.answer(),
                request.questionType(),
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public InterrogationContext loadContext(Long sessionId, InterrogationRequest request) {
        if (!playSessionReader.isPlaying(sessionId)) {
            throw new AiException(AiErrorCode.INTERROGATION_SESSION_NOT_PLAYING);
        }

        SuspectProfile suspect = suspectReader.findById(request.suspectId());

        List<Long> unlockedEvidenceIds = evidenceReader.getUnlockedEvidenceIds(sessionId);
        List<EvidenceInfo> revealedEvidences = evidenceReader.getUnlockedEvidences(sessionId);

        if (request.presentedEvidenceId() != null && !unlockedEvidenceIds.contains(request.presentedEvidenceId())) {
            throw new AiException(AiErrorCode.INTERROGATION_EVIDENCE_NOT_UNLOCKED);
        }

        EvidenceInfo presentedEvidence = request.presentedEvidenceId() != null
                ? evidenceReader.findById(request.presentedEvidenceId())
                : null;

        ResponsePolicyResult policy = policyResolver.resolve(
                request.suspectId(), unlockedEvidenceIds, request.presentedEvidenceId());

        List<ChatTurn> history = historyProvider.getHistory(
                sessionId, request.suspectId(), maxHistoryTurns);

        return new InterrogationContext(suspect, revealedEvidences, presentedEvidence, policy, history);
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

    public record InterrogationContext(
            SuspectProfile suspect,
            List<EvidenceInfo> revealedEvidences,
            EvidenceInfo presentedEvidence,
            ResponsePolicyResult policy,
            List<ChatTurn> history
    ) {}
}
