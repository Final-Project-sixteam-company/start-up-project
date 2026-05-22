package com.startup.domain.notification.dto;

import com.startup.domain.notification.entity.DeviceToken;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "디바이스 토큰 응답")
// 앱은 저장된 token 원문이 아니라 식별자와 활성 상태만 확인하면 된다.
public record DeviceTokenResponse(
        @Schema(description = "디바이스 토큰 식별자", example = "1")
        Long deviceTokenId,

        @Schema(description = "활성 여부", example = "true")
        boolean active
) {
    public static DeviceTokenResponse from(DeviceToken deviceToken) {
        // Entity를 Controller에 직접 노출하지 않기 위한 응답 변환 지점이다.
        return new DeviceTokenResponse(
                deviceToken.getId(),
                deviceToken.isActive()
        );
    }
}
