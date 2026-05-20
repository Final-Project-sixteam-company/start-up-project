package com.startup.domain.ai.support;

/**
 * 힌트 패널티 조회 인터페이스.
 *
 * 반환 계약:
 * - null을 반환하지 않는다.
 * - 누적 패널티가 없으면 0을 반환한다.
 */
public interface HintPenaltyReader {

    int getTotalPenalty(Long sessionId);
}
