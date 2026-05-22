package com.startup.domain.notification.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.notification.dto.DeviceTokenRegisterRequest;
import com.startup.domain.notification.dto.DeviceTokenResponse;
import com.startup.domain.notification.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Device Token", description = "FCM 디바이스 토큰 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "디바이스 토큰 등록", description = "Android에서 발급받은 FCM registration token을 등록한다.")
    @PostMapping
    public ResponseEntity<ApiResponse<DeviceTokenResponse>> register(
            @Valid @RequestBody DeviceTokenRegisterRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        DeviceTokenResponse response = deviceTokenService.register(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
