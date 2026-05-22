package com.startup.domain.notification.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.domain.notification.service.DeviceTokenService;
import com.startup.domain.notification.service.FcmNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
        context.registerBean(DeviceTokenService.class, () -> mock(DeviceTokenService.class));
        context.registerBean(FcmNotificationService.class, () -> mock(FcmNotificationService.class));
        context.registerBean(MockUserProvider.class, () -> new MockUserProvider(1L));
    }
}
