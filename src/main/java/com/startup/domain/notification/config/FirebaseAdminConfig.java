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
public class FirebaseAdminConfig {

    @Bean
    @ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
    public FirebaseApp firebaseApp(
            @Value("${fcm.service-account-path:}") String serviceAccountPath,
            @Value("${fcm.project-id:}") String projectId
    ) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        if (!StringUtils.hasText(serviceAccountPath)) {
            throw new IllegalStateException("FCM service account path is required when fcm.enabled=true.");
        }

        try (InputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount));

            if (StringUtils.hasText(projectId)) {
                builder.setProjectId(projectId);
            }

            return FirebaseApp.initializeApp(builder.build());
        }
    }
}
