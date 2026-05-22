package com.startup.domain.notification.error;

import com.startup.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
// notification 도메인에서 발생하는 비즈니스 오류를 공통 ApiResponse 형식으로 변환하기 위한 코드다.
public enum NotificationErrorCode implements ErrorCode {
    FCM_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "N001", "FCM is disabled."),
    FCM_SEND_FAILED(HttpStatus.BAD_GATEWAY, "N002", "FCM push send failed."),
    DEVICE_TOKEN_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "N003", "Device token save failed.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
