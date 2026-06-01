package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.SolutionInfo;

/**
 * 정답 정보 조회 인터페이스.
 *
 * 반환 계약:
 * - null을 반환하지 않는다.
 * - 조회 대상이 없으면 Optional보다 AiException 사용을 우선한다.
 */
public interface SolutionReader {

    SolutionInfo findByScenarioId(Long scenarioId);

    SolutionInfo findByScenarioIdAndVariantId(Long scenarioId, Long variantId);
}
