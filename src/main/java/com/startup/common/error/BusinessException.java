package com.startup.common.error;

import lombok.Getter;

import java.util.Map;

@Getter
// 서비스/도메인 계층에서 의도적으로 던지는 예외의 기본 타입이다.
// ErrorCode를 함께 보관해 GlobalExceptionHandler가 일관된 응답으로 변환한다.
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }
}
