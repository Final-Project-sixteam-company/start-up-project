package com.startup.domain.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.startup.domain.notification.error.NotificationErrorCode;
import com.startup.domain.notification.error.NotificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmNotificationService {

    private static final int MAX_MULTICAST_TOKENS = 500;

    private final ObjectProvider<FirebaseApp> firebaseAppProvider;
    private final boolean enabled;

    public FcmNotificationService(
            ObjectProvider<FirebaseApp> firebaseAppProvider,
            @Value("${fcm.enabled:false}") boolean enabled
    ) {
        this.firebaseAppProvider = firebaseAppProvider;
        this.enabled = enabled;
    }

    public void sendToToken(String token, String title, String body) {
        FirebaseApp firebaseApp = getFirebaseApp();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance(firebaseApp).send(message);
        } catch (FirebaseMessagingException e) {
            throw new NotificationException(NotificationErrorCode.FCM_SEND_FAILED, e);
        }
    }

    public int sendToTokens(List<String> tokens, String title, String body) {
        if (tokens == null || tokens.isEmpty()) {
            log.debug("No active device tokens for test push.");
            return 0;
        }

        FirebaseApp firebaseApp = getFirebaseApp();
        int successCount = 0;

        for (int start = 0; start < tokens.size(); start += MAX_MULTICAST_TOKENS) {
            int end = Math.min(start + MAX_MULTICAST_TOKENS, tokens.size());
            List<String> chunk = tokens.subList(start, end);
            successCount += sendChunk(firebaseApp, chunk, title, body);
        }

        return successCount;
    }

    private int sendChunk(FirebaseApp firebaseApp, List<String> tokens, String title, String body) {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance(firebaseApp).sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                log.warn("FCM multicast partial failure: successCount={}, failureCount={}",
                        response.getSuccessCount(), response.getFailureCount());
            }
            return response.getSuccessCount();
        } catch (FirebaseMessagingException e) {
            throw new NotificationException(NotificationErrorCode.FCM_SEND_FAILED, e);
        }
    }

    private FirebaseApp getFirebaseApp() {
        if (!enabled) {
            throw new NotificationException(NotificationErrorCode.FCM_DISABLED);
        }

        FirebaseApp firebaseApp = firebaseAppProvider.getIfAvailable();
        if (firebaseApp == null) {
            throw new NotificationException(NotificationErrorCode.FCM_DISABLED);
        }

        return firebaseApp;
    }
}
