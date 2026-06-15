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
    SCENARIO_NOT_MODIFY(HttpStatus.BAD_REQUEST, "SCENARIO_006", "이미 발행된 시나리오는 수정할 수 없습니다."),
    EVIDENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_007", "증거를 찾을 수 없습니다."),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_008", "장소를 찾을 수 없습니다."),
    INVALID_LOCATION_OWNERSHIP(HttpStatus.BAD_REQUEST, "SCENARIO_009", "다른 시나리오의 장소를 지정할 수 없습니다."),
    INVALID_SUSPECT_OWNERSHIP(HttpStatus.BAD_REQUEST, "SCENARIO_010", "다른 시나리오의 용의자를 지정할 수 없습니다."),
    INVALID_EVIDENCE_OWNERSHIP(HttpStatus.BAD_REQUEST, "SCENARIO_011", "다른 시나리오의 증거를 지정할 수 없습니다."),
    SUSPECT_IS_CULPRIT(HttpStatus.BAD_REQUEST, "SCENARIO_012", "정답(범인)으로 지목된 용의자는 삭제할 수 없습니다."),
    SUSPECT_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_013", "용의자를 찾을 수 없습니다."),
    EVIDENCE_IS_KEY(HttpStatus.BAD_REQUEST, "SCENARIO_014", "정답(핵심 증거)으로 지목된 증거는 삭제할 수 없습니다."),
    SOLUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_015", "시나리오 정답을 찾을 수 없습니다."),
    EVIDENCE_IS_PREREQUISITE(HttpStatus.BAD_REQUEST, "SCENARIO_016", "다른 증거의 해금 조건으로 사용 중인 증거는 삭제할 수 없습니다."),
    SUSPECT_IS_PREREQUISITE(HttpStatus.BAD_REQUEST, "SCENARIO_017", "특정 증거의 해금 조건(대상 인물)으로 사용 중인 용의자는 삭제할 수 없습니다."),
    VICTIM_NOT_FOUND(HttpStatus.NOT_FOUND, "SCENARIO_018", "피해자 정보가 등록되지 않았습니다."),
    INVALID_EVIDENCE_CONDITION(HttpStatus.BAD_REQUEST, "SCENARIO_019", "잘못된 증거 해금 조건입니다."),
    SCENARIO_ALREADY_DELETED(HttpStatus.NOT_FOUND, "SCENARIO_020", "삭제된 시나리오입니다."),
    SCENARIO_CANNOT_HIDE(HttpStatus.BAD_REQUEST, "SCENARIO_021", "현재 상태에서는 시나리오를 비공개 처리할 수 없습니다."),
    SCENARIO_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "SCENARIO_022", "시나리오를 삭제할 수 없는 상태입니다."),
    EVIDENCE_USED_IN_POLICY(HttpStatus.BAD_REQUEST, "SCENARIO_023", "답변 정책에서 참조 중인 증거는 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
