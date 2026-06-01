package com.startup.domain.play.support;

import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.support.EvidenceReader;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.play.service.TimeEvidenceUnlockSyncer;
import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class DefaultEvidenceReader implements EvidenceReader {

    private final UnlockedEvidenceRepository unlockedEvidenceRepository;
    private final EvidenceRepository evidenceRepository;
    private final TimeEvidenceUnlockSyncer timeEvidenceUnlockSyncer;
    private final PlaySessionRepository playSessionRepository;
    private final EvidenceVariantDescriptionResolver evidenceVariantDescriptionResolver;

    @Override
    public List<Long> getUnlockedEvidenceIds(Long sessionId) {
        return unlockedEvidenceRepository.findAllByPlaySessionId(sessionId)
                .stream()
                .map(unlocked -> unlocked.getEvidenceId())
                .toList();
    }

    @Override
    public List<EvidenceInfo> getUnlockedEvidences(Long sessionId) {
        List<Long> unlockedIds = getUnlockedEvidenceIds(sessionId);
        if (unlockedIds.isEmpty()) return List.of();
        Long variantId = resolveVariantId(sessionId);
        return evidenceRepository.findAllById(unlockedIds)
                .stream()
                .map(evidence -> new EvidenceInfo(
                        evidence.getId(),
                        evidence.getTitle(),
                        evidenceVariantDescriptionResolver.resolve(evidence, variantId)
                ))
                .toList();
    }

    @Override
    public EvidenceInfo findById(Long evidenceId) {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new AiException(AiErrorCode.INTERROGATION_EVIDENCE_NOT_UNLOCKED));
        return new EvidenceInfo(evidence.getId(), evidence.getTitle(), evidence.getDescription());
    }

    @Override
    public EvidenceInfo findById(Long sessionId, Long evidenceId) {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new AiException(AiErrorCode.INTERROGATION_EVIDENCE_NOT_UNLOCKED));
        return new EvidenceInfo(
                evidence.getId(),
                evidence.getTitle(),
                evidenceVariantDescriptionResolver.resolve(evidence, resolveVariantId(sessionId))
        );
    }

    private Long resolveVariantId(Long sessionId) {
        return playSessionRepository.findById(sessionId)
                .map(PlaySession::getScenarioVariantId)
                .orElse(null);
    }
}
