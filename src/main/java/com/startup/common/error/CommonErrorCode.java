package com.startup.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
// C 계열 코드는 도메인에 종속되지 않는 공통 HTTP/요청 오류에 사용한다.
public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid input value."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C002", "Invalid request."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C003", "Authentication is required."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C004", "Access is denied."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "Resource not found."),
    CONFLICT(HttpStatus.CONFLICT, "C006", "Request conflict."),
    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "C007", "Payload is too large."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C008", "HTTP method is not allowed."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C009", "Unsupported media type."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C010", "Internal server error.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
