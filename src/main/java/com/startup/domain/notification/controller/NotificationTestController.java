package com.startup.domain.notification.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.notification.dto.TestPushRequest;
import com.startup.domain.notification.service.DeviceTokenService;
import com.startup.domain.notification.service.FcmNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notification Test", description = "개발/검증용 푸시 알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationTestController {

    private final DeviceTokenService deviceTokenService;
    private final FcmNotificationService fcmNotificationService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "테스트 푸시 발송", description = "현재 MockUser의 활성 디바이스 토큰으로 테스트 푸시를 보낸다.")
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Void>> sendTestPush(
            @Valid @RequestBody TestPushRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<String> tokens = deviceTokenService.getActiveTokens(userId);
        fcmNotificationService.sendToTokens(tokens, request.title(), request.body());
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
