package com.startup.domain.ai.error;

import com.startup.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {
    AI_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI001", "AI 요청에 실패했습니다"),
    AI_RESPONSE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI002", "AI 응답 파싱에 실패했습니다"),
    PROMPT_BUILD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI003", "프롬프트 생성에 실패했습니다"),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI004", "AI 서비스를 사용할 수 없습니다"),
    AI_RESPONSE_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI005", "AI 응답 시간이 초과되었습니다"),
    AI_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "AI006", "AI 응답이 유효하지 않습니다"),
    INTERROGATION_SESSION_NOT_PLAYING(HttpStatus.BAD_REQUEST, "AI007", "플레이 중인 세션이 아닙니다"),
    INTERROGATION_SUSPECT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI008", "용의자를 찾을 수 없습니다"),
    INTERROGATION_EVIDENCE_NOT_UNLOCKED(HttpStatus.BAD_REQUEST, "AI009", "제시한 증거가 아직 해금되지 않았습니다"),
    FINAL_DEDUCTION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "AI010", "이미 최종 추리를 제출했습니다"),
    SOLUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "AI011", "시나리오 정답 정보를 찾을 수 없습니다"),
    SCORING_CRITERIA_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "AI012", "채점 기준 정보를 찾을 수 없습니다"),
    DEDUCTION_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI013", "채점 결과를 찾을 수 없습니다"),
    SCORING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI014", "채점 처리 중 오류가 발생했습니다. 다시 시도해 주세요"),
    SCORING_IN_PROGRESS(HttpStatus.CONFLICT, "AI015", "채점이 진행 중입니다. 잠시 후 다시 시도해 주세요"),
    SCENARIO_VALIDATION_IN_PROGRESS(HttpStatus.CONFLICT, "AI016", "시나리오 검증이 진행 중입니다"),
    SCENARIO_VALIDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI017", "시나리오 검증 처리 중 오류가 발생했습니다"),
    SCENARIO_VALIDATION_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "AI018", "시나리오 검증 결과를 찾을 수 없습니다"),
    SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "AI019", "시나리오를 찾을 수 없습니다"),
    SCENARIO_DATA_INCOMPLETE(HttpStatus.BAD_REQUEST, "AI020", "시나리오 데이터가 불완전합니다"),
    FINAL_DEDUCTION_EVIDENCE_NOT_UNLOCKED(HttpStatus.BAD_REQUEST, "AI021", "최종 추리에 사용할 수 없는 증거입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
