package com.startup.domain.ai.support;

public interface PlaySessionCompleter {

    /**
     * 최종 추리 채점을 시작하기 위해 in-flight lock을 획득한다.
     *
     * @param sessionId 플레이 세션 ID
     * @throws com.startup.domain.ai.error.AiException 이미 제출했거나 채점이 진행 중인 경우
     */
    void lockForFinalDeduction(Long sessionId);

    /**
     * 최종 추리 저장 성공 후 세션을 완료 처리한다.
     *
     * @param sessionId 플레이 세션 ID
     */
    void complete(Long sessionId);

    /**
     * 채점 실패 또는 TX2 저장 실패 시 in-flight lock을 해제한다.
     * completed 상태는 변경하지 않는다.
     *
     * @param sessionId 플레이 세션 ID
     */
    void releaseFinalDeductionLock(Long sessionId);
}
