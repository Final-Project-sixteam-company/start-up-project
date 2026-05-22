package com.startup.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "디바이스 토큰 등록 요청")
public record DeviceTokenRegisterRequest(
        @Schema(description = "FCM registration token", example = "fcm_registration_token")
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        @Size(max = 512, message = "FCM 토큰은 512자 이하로 입력해 주세요.")
        String token,

        @Schema(description = "디바이스 타입", example = "ANDROID")
        @Size(max = 30, message = "디바이스 타입은 30자 이하로 입력해 주세요.")
        String deviceType
) {
}
