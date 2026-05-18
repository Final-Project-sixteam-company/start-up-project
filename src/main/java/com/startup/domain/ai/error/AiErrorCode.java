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
    PROMPT_BUILD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI003", "프롬프트 생성에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
