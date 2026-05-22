package com.startup.domain.notification.dto;

import com.startup.domain.notification.entity.DeviceToken;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "디바이스 토큰 응답")
public record DeviceTokenResponse(
        @Schema(description = "디바이스 토큰 식별자", example = "1")
        Long deviceTokenId,

        @Schema(description = "활성 여부", example = "true")
        boolean active
) {
    public static DeviceTokenResponse from(DeviceToken deviceToken) {
        return new DeviceTokenResponse(
                deviceToken.getId(),
                deviceToken.isActive()
        );
    }
}
