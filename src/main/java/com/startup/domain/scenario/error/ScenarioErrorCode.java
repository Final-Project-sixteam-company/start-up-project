package com.startup.domain.scenario.error;

import com.startup.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScenarioErrorCode implements ErrorCode {

    SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_001", "시나리오를 찾을 수 없습니다."),
    SCENARIO_CANNOT_PUBLISH(HttpStatus.BAD_REQUEST, "SCENARIO_002", "현재 상태에서는 시나리오를 공개할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
