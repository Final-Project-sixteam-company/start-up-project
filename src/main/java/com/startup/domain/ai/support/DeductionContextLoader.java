package com.startup.domain.ai.support;

import com.startup.domain.ai.entity.FinalDeduction;
import com.startup.domain.ai.entity.FinalDeductionEvidence;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.repository.FinalDeductionEvidenceRepository;
import com.startup.domain.ai.repository.FinalDeductionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeductionContextLoader {

    private final PlaySessionCompleter playSessionCompleter;
    private final FinalDeductionRepository finalDeductionRepository;
    private final FinalDeductionEvidenceRepository finalDeductionEvidenceRepository;

    @Transactional
    public void ensureNotSubmitted(Long sessionId) {
        if (finalDeductionRepository.existsByPlaySessionId(sessionId)) {
            throw new AiException(AiErrorCode.FINAL_DEDUCTION_ALREADY_SUBMITTED);
        }
        playSessionCompleter.lockForFinalDeduction(sessionId);
    }

    @Transactional
    public FinalDeduction saveResultAndComplete(Long sessionId, FinalDeduction entity,
                                                 List<Long> selectedEvidenceIds) {
        FinalDeduction saved = finalDeductionRepository.save(entity);
        List<FinalDeductionEvidence> evidences = selectedEvidenceIds.stream()
                .distinct()
                .map(evidenceId -> new FinalDeductionEvidence(saved.getId(), evidenceId))
                .toList();
        finalDeductionEvidenceRepository.saveAll(evidences);
        playSessionCompleter.complete(sessionId);
        return saved;
    }

    @Transactional(readOnly = true)
    public FinalDeduction findBySessionId(Long sessionId) {
        return finalDeductionRepository.findByPlaySessionId(sessionId).orElse(null);
    }
}
