package com.startup.domain.notification.error;

import com.startup.common.error.BusinessException;

public class NotificationException extends BusinessException {

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode);
    }

    public NotificationException(NotificationErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
