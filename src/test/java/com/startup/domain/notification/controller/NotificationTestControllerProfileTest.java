package com.startup.domain.notification.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.domain.notification.service.DeviceTokenService;
import com.startup.domain.notification.service.FcmNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// 테스트 푸시 API가 prod profile에 노출되지 않는지 확인하는 안전장치 테스트다.
class NotificationTestControllerProfileTest {

    @Test
    void notificationTestControllerIsNotRegisteredInProdProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("prod");
            registerDependencies(context);
            context.register(NotificationTestController.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(NotificationTestController.class)).isEmpty();
        }
    }

    @Test
    void notificationTestControllerIsRegisteredOutsideProdProfile() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local");
            registerDependencies(context);
            context.register(NotificationTestController.class);
            context.refresh();

            assertThat(context.getBeanNamesForType(NotificationTestController.class)).hasSize(1);
        }
    }

    private void registerDependencies(AnnotationConfigApplicationContext context) {
        // 컨트롤러의 profile 조건만 검증하려고 실제 서비스 대신 mock bean을 등록한다.
        context.registerBean(DeviceTokenService.class, () -> mock(DeviceTokenService.class));
        context.registerBean(FcmNotificationService.class, () -> mock(FcmNotificationService.class));
        context.registerBean(MockUserProvider.class, () -> new MockUserProvider(1L));
    }
}
