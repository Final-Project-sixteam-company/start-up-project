package com.startup.domain.example.error;

import com.startup.common.error.BusinessException;

// Example 도메인에서 발생한 의도된 예외를 GlobalExceptionHandler까지 전달한다.
public class ExampleException extends BusinessException {

    public ExampleException(ExampleErrorCode errorCode) {
        super(errorCode);
    }

    public ExampleException(ExampleErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
