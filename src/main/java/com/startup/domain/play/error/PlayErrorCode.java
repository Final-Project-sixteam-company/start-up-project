package com.startup.domain.play.error;

import com.startup.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
// 게임 플레이 도메인 전용 에러 코드
public enum PlayErrorCode implements ErrorCode {

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "플레이 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "P002", "이미 진행 중인 플레이 세션이 존재합니다."),
    SESSION_NOT_PLAYING(HttpStatus.BAD_REQUEST, "P03", "진행 중인 세션이 아닙니다."),
    SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P004", "해당 세션에 접근할 수 없습니다."),
    EVIDENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "P005", "증거를 찾을 수 없습니다."),
    EVIDENCE_ALREADY_UNLOCKED(HttpStatus.CONFLICT, "P006", "이미 해금된 증거입니다."),
    SCENARIO_NOT_PLAYABLE(HttpStatus.BAD_REQUEST, "P007", "플레이할 수 없는 시나리오입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
