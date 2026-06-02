package com.startup.domain.scenario.service;

import com.startup.domain.scenario.entity.ScenarioVariant;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.ScenarioVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioVariantService {

    private final ScenarioVariantRepository scenarioVariantRepository;

    // 특정 시나리오의 변형(Variant)을 활성화하는 서비스 로직
    @Transactional
    public void activateVariant(Long scenarioId, Long variantId) {
        // Variant 조회
        ScenarioVariant variant = scenarioVariantRepository.findById(variantId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.VARIANT_NOT_FOUND));

        // 소속 검증
        if (!variant.getScenarioId().equals(scenarioId)) {
            throw new ScenarioException(ScenarioErrorCode.VARIANT_SCENARIO_MISMATCH);
        }

        // 기존에 켜져 있던 같은 시나리오의 모든 Variant를 강제로 끈다
        scenarioVariantRepository.deactivateAllByScenarioId(scenarioId);

        variant.activate();
    }

}
