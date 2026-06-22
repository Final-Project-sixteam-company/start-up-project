package com.startup.domain.scenario.service;

import com.startup.domain.scenario.entity.ScenarioVariant;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.repository.ScenarioVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioVariantService {

    private final ScenarioVariantRepository scenarioVariantRepository;
    private final ScenarioRepository scenarioRepository;

    /**
     * 특정 시나리오의 변형(Variant)을 단독으로 활성화하는 스튜디오/관리용 서비스 로직.
     * 주의: 이 메서드는 선택한 Variant 외의 나머지 모든 Variant를 강제 비활성화합니다.
     * 따라서 여러 개의 범인(Variant)이 활성화되어 가중치 기반으로 랜덤 샘플링되어야 하는
     * 공식 시나리오에서는 이 메서드를 호출하면 안 됩니다.
     * 오직 '단일 Active Variant' 정책을 가지는 커스텀 시나리오나 특정 관리 목적에만 사용해야 합니다.
     */
    @Transactional
    public void activateVariant(Long scenarioId, Long variantId) {
        //부모인 시나리오에 배타적 락을 걸어 동시 접근 차단
        scenarioRepository.findByIdForUpdate(scenarioId).orElseThrow(() ->
                new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        // Variant 조회
        ScenarioVariant variant = scenarioVariantRepository.findById(variantId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.VARIANT_NOT_FOUND));

        // 소속 검증
        if (!variant.getScenarioId().equals(scenarioId)) {
            throw new ScenarioException(ScenarioErrorCode.VARIANT_SCENARIO_MISMATCH);
        }

        // 내가 선택한 variant를 제외한 나머지 모두 끈다
        scenarioVariantRepository.deactivateAllByScenarioId(scenarioId, variantId);

        //요청한 variant를 켠다
        variant.activate();
    }

}
