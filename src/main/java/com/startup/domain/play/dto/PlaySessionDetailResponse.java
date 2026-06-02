package com.startup.domain.play.dto;

import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.enums.PlaySessionStatus;

import java.time.Duration;
import java.time.LocalDateTime;

// 세션 기본 정보 및 타이머 동기화 응답 DTO
public record PlaySessionDetailResponse(
        Long sessionId,
        Long scenarioId,
        PlaySessionStatus status,
        int elapsedSeconds, // 프론트엔드 타이머용 경과 시간 (초)
        LocalDateTime startedAt
) {
    public static PlaySessionDetailResponse from(PlaySession session) {
        // 게임 진행 중이면 '현재 시간 - 시작 시간'으로 계산하고,
        // 종료(포기/완료) 상태면 DB에 저장된 최종 시간을 내려줍니다.
        int calculatedElapsedSeconds;
        if (session.isPlaying()) {
            calculatedElapsedSeconds = (int) Duration.between(session.getStartedAt(), LocalDateTime.now()).getSeconds();
        } else {
            calculatedElapsedSeconds = session.getCurrentElapsedSeconds();
        }

        return new PlaySessionDetailResponse(
                session.getId(),
                session.getScenarioId(),
                session.getStatus(),
                calculatedElapsedSeconds,
                session.getStartedAt()
        );
    }
}
