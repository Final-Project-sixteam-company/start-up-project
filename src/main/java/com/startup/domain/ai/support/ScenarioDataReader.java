package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ScenarioValidationData;

/**
 * 시나리오 검증용 전체 데이터 조회 인터페이스.
 *
 * 이 Reader는 제작자용 검증 API 전용이다.
 * Solution, SuspectSecret 등 정답 데이터를 포함하므로
 * 플레이 API나 심문 프롬프트에서 절대 사용하지 않는다.
 */
public interface ScenarioDataReader {

    /**
     * 시나리오 검증에 필요한 전체 데이터를 조회한다.
     *
     * @param scenarioId 시나리오 ID
     * @return 시나리오 전체 데이터 (non-null)
     * @throws com.startup.domain.ai.error.AiException 시나리오가 없는 경우
     */
    ScenarioValidationData loadForValidation(Long scenarioId);
}
