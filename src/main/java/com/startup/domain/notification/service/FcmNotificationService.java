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
// FirebaseMessaging 호출을 한 곳에 모아 컨트롤러/토큰 서비스가 Firebase SDK에 직접 의존하지 않게 한다.
public class FcmNotificationService {

    // Firebase Admin SDK multicast 전송은 요청당 최대 500개 registration token을 지원한다.
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

        // MVP 알림은 notification title/body만 사용하고, data payload/deep link는 후속 작업으로 남긴다.
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

        // registration token이 500개를 넘으면 Firebase 제한에 맞춰 여러 번 나누어 전송한다.
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
                // 실패 token cleanup은 후속 작업으로 분리하고, MVP에서는 실패 수만 로그로 남긴다.
                log.warn("FCM multicast partial failure: successCount={}, failureCount={}",
                        response.getSuccessCount(), response.getFailureCount());
            }
            return response.getSuccessCount();
        } catch (FirebaseMessagingException e) {
            throw new NotificationException(NotificationErrorCode.FCM_SEND_FAILED, e);
        }
    }

    private FirebaseApp getFirebaseApp() {
        // FCM 비활성 환경에서는 실수로 외부 발송이 나가지 않도록 명시적으로 실패시킨다.
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
