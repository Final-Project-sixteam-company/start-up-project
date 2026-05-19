package com.startup.domain.ai.support;

import com.startup.domain.ai.entity.FinalDeduction;
import com.startup.domain.ai.entity.FinalDeductionEvidence;
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
    public void lockAndComplete(Long sessionId) {
        playSessionCompleter.lockAndComplete(sessionId);
    }

    @Transactional
    public FinalDeduction saveResult(FinalDeduction entity, List<Long> selectedEvidenceIds) {
        FinalDeduction saved = finalDeductionRepository.save(entity);
        List<FinalDeductionEvidence> evidences = selectedEvidenceIds.stream()
                .map(evidenceId -> new FinalDeductionEvidence(saved.getId(), evidenceId))
                .toList();
        finalDeductionEvidenceRepository.saveAll(evidences);
        return saved;
    }

    @Transactional(readOnly = true)
    public FinalDeduction findBySessionId(Long sessionId) {
        return finalDeductionRepository.findByPlaySessionId(sessionId).orElse(null);
    }
}
