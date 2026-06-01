package com.startup.domain.scenario.service;

import com.startup.domain.scenario.entity.ScenarioVariant;
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
        // 기존에 켜져 있던 같은 시나리오의 모든 Variant를 강제로 끈다 (동시성 및 휴먼 에러 방어)
        scenarioVariantRepository.deactivateAllByScenarioId(scenarioId);

        // 선택한 Variant만 켠다
        ScenarioVariant variant = scenarioVariantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found"));

        variant.activate(); // 영속성 컨텍스트 변경 감지(Dirty Checking)로 UPDATE 쿼리 발생
    }

}
