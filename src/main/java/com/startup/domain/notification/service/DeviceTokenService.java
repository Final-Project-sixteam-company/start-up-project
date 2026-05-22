package com.startup.domain.notification.service;

import com.startup.domain.notification.dto.DeviceTokenRegisterRequest;
import com.startup.domain.notification.dto.DeviceTokenResponse;
import com.startup.domain.notification.entity.DeviceToken;
import com.startup.domain.notification.error.NotificationErrorCode;
import com.startup.domain.notification.error.NotificationException;
import com.startup.domain.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
// 디바이스 토큰 등록/조회 규칙을 담당하고, FCM 외부 호출은 이 서비스에서 수행하지 않는다.
public class DeviceTokenService {

    private static final String DEFAULT_DEVICE_TYPE = "ANDROID";

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public DeviceTokenResponse register(Long userId, DeviceTokenRegisterRequest request) {
        String token = request.token().trim();
        String deviceType = normalizeDeviceType(request.deviceType());

        // token unique 기준으로 신규 등록과 재등록을 같은 경로에서 처리한다.
        deviceTokenRepository.upsertActiveToken(userId, token, deviceType);

        // native upsert 후 DB 값을 다시 읽어 Android에 안정적인 id/active 상태를 반환한다.
        return deviceTokenRepository.findByToken(token)
                .map(DeviceTokenResponse::from)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.DEVICE_TOKEN_SAVE_FAILED));
    }

    @Transactional(readOnly = true)
    public List<String> getActiveTokens(Long userId) {
        // Controller가 발송 전에 token 원문 목록만 필요로 하므로 Entity 대신 String 목록으로 반환한다.
        return deviceTokenRepository.findAllByUserIdAndActiveTrue(userId)
                .stream()
                .map(DeviceToken::getToken)
                .toList();
    }

    private String normalizeDeviceType(String deviceType) {
        if (deviceType == null || deviceType.isBlank()) {
            return DEFAULT_DEVICE_TYPE;
        }
        // 클라이언트 입력 대소문자 차이로 같은 디바이스 타입이 갈라지지 않게 정규화한다.
        return deviceType.trim().toUpperCase(Locale.ROOT);
    }
}
