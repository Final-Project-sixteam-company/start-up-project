package com.startup.domain.scenario.error;

import com.startup.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScenarioErrorCode implements ErrorCode {

    SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_001", "시나리오를 찾을 수 없습니다."),
    SCENARIO_CANNOT_PUBLISH(HttpStatus.BAD_REQUEST, "SCENARIO_002", "현재 상태에서는 시나리오를 공개할 수 없습니다."),
    VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_003", "시나리오 변형(Variant)을 찾을 수 없습니다."),
    VARIANT_SCENARIO_MISMATCH(HttpStatus.BAD_REQUEST, "SCENARIO_004", "해당 Variant는 요청한 시나리오에 속하지 않습니다."),
    SCENARIO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SCENARIO_005", "해당 시나리오에 대한 접근 권한이 없습니다."),
    SCENARIO_NOT_MODIFY(HttpStatus.BAD_REQUEST, "SCENARIO_006", "이미 발행된 시나리오는 수정할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
