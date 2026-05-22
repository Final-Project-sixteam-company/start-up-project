package com.startup.domain.notification.error;

import com.startup.common.error.BusinessException;

// GlobalExceptionHandler가 ErrorCode 기반 응답으로 처리할 수 있게 하는 notification 전용 예외다.
public class NotificationException extends BusinessException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(NotificationErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
