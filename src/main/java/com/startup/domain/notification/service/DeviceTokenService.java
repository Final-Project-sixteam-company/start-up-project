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
public class DeviceTokenService {

    private static final String DEFAULT_DEVICE_TYPE = "ANDROID";

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public DeviceTokenResponse register(Long userId, DeviceTokenRegisterRequest request) {
        String token = request.token().trim();
        String deviceType = normalizeDeviceType(request.deviceType());

        deviceTokenRepository.upsertActiveToken(userId, token, deviceType);

        return deviceTokenRepository.findByToken(token)
                .map(DeviceTokenResponse::from)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.DEVICE_TOKEN_SAVE_FAILED));
    }

    @Transactional(readOnly = true)
    public List<String> getActiveTokens(Long userId) {
        return deviceTokenRepository.findAllByUserIdAndActiveTrue(userId)
                .stream()
                .map(DeviceToken::getToken)
                .toList();
    }

    private String normalizeDeviceType(String deviceType) {
        if (deviceType == null || deviceType.isBlank()) {
            return DEFAULT_DEVICE_TYPE;
        }
        return deviceType.trim().toUpperCase(Locale.ROOT);
    }
}
