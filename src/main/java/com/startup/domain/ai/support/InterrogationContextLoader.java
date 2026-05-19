package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.InterrogationContext;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InterrogationContextLoader {

    private final PlaySessionReader playSessionReader;
    private final SuspectReader suspectReader;
    private final EvidenceReader evidenceReader;
    private final ResponsePolicyResolver policyResolver;
    private final InterrogationHistoryProvider historyProvider;

    @Value("${caselab.ai.interrogation.max-history-turns:5}")
    private int maxHistoryTurns;

    @Transactional(readOnly = true)
    public InterrogationContext load(Long sessionId, Long suspectId,
                                    Long presentedEvidenceId) {
        if (!playSessionReader.isPlaying(sessionId)) {
            throw new AiException(AiErrorCode.INTERROGATION_SESSION_NOT_PLAYING);
        }

        SuspectProfile suspect = suspectReader.findById(suspectId);

        List<Long> unlockedEvidenceIds = evidenceReader.getUnlockedEvidenceIds(sessionId);
        List<EvidenceInfo> revealedEvidences = evidenceReader.getUnlockedEvidences(sessionId);

        if (presentedEvidenceId != null && !unlockedEvidenceIds.contains(presentedEvidenceId)) {
            throw new AiException(AiErrorCode.INTERROGATION_EVIDENCE_NOT_UNLOCKED);
        }

        EvidenceInfo presentedEvidence = presentedEvidenceId != null
                ? evidenceReader.findById(presentedEvidenceId)
                : null;

        ResponsePolicyResult policy = policyResolver.resolve(
                suspectId, unlockedEvidenceIds, presentedEvidenceId);

        List<ChatTurn> history = historyProvider.getHistory(
                sessionId, suspectId, maxHistoryTurns);

        return new InterrogationContext(suspect, revealedEvidences, presentedEvidence, policy, history);
    }
}
