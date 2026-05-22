package com.startup.domain.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
// Firebase Admin SDK 초기화만 담당해 FCM 발송 서비스가 인증 파일 처리에 관여하지 않게 한다.
public class FirebaseAdminConfig {

    @Bean
    @ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
    // FCM을 끈 환경(local/test 기본값)에서는 service account 파일 없이도 서버가 부팅되어야 한다.
    public FirebaseApp firebaseApp(
            @Value("${fcm.service-account-path:}") String serviceAccountPath,
            @Value("${fcm.project-id:}") String projectId
    ) throws IOException {
        // Firebase Admin SDK는 전역 app registry를 쓰므로 재시작/테스트 중 중복 초기화를 피한다.
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        if (!StringUtils.hasText(serviceAccountPath)) {
            throw new IllegalStateException("FCM service account path is required when fcm.enabled=true.");
        }

        // non-Google 서버 환경에서는 mounted service account JSON을 명시적으로 읽어 인증한다.
        try (InputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));

            // projectId는 credentials에 들어있을 수 있지만, 운영 설정을 명확히 하기 위해 값이 있으면 지정한다.
            if (StringUtils.hasText(projectId)) {
                builder.setProjectId(projectId);
            }

            return FirebaseApp.initializeApp(builder.build());
        }
    }
}
