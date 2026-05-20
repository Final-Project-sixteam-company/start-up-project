package com.startup.domain.ai.support;

/**
 * PlaySession 조회 인터페이스.
 * 소수경의 play 도메인 구현 후 실제 구현체로 교체한다.
 *
 * 반환 계약:
 * - null을 반환하지 않는다.
 * - 조회 대상이 없으면 Optional보다 AiException 사용을 우선한다.
 */
public interface PlaySessionReader {

    boolean isPlaying(Long sessionId);

    Long getScenarioId(Long sessionId);
}
