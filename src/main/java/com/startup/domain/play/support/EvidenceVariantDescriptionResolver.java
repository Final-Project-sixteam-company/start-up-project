package com.startup.domain.play.support;

import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.entity.EvidenceVariantState;
import com.startup.domain.scenario.repository.EvidenceVariantStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvidenceVariantDescriptionResolver {

    private final EvidenceVariantStateRepository evidenceVariantStateRepository;

    public String resolve(Evidence evidence, Long variantId) {
        if (evidence == null) {
            return null;
        }
        String baseDescription = evidence.getDescription();
        if (variantId == null) {
            return baseDescription;
        }

        return evidenceVariantStateRepository.findByVariantIdAndEvidenceId(variantId, evidence.getId())
                .map(state -> applyState(baseDescription, state))
                .orElse(baseDescription);
    }

    private String applyState(String baseDescription, EvidenceVariantState state) {
        if (hasText(state.getDetailOverride())) {
            return state.getDetailOverride();
        }
        if (hasText(state.getDetailAppend())) {
            if (!hasText(baseDescription)) {
                return state.getDetailAppend();
            }
            return baseDescription + "\n\n" + state.getDetailAppend();
        }
        return baseDescription;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
