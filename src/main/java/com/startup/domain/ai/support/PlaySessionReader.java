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

    /**
     * 플레이 세션 소유자 ID를 조회한다.
     * 반환 계약:
     * - null을 반환하지 않는다.
     * - 세션이 없으면 명시적 예외를 던진다.
     * - MVP Mock 단계에서는 app.mock-user-id와 동일한 ID를 반환할 수 있다.
     */
    Long getOwnerUserId(Long sessionId);

    Long getScenarioVariantId(Long sessionId);
}
