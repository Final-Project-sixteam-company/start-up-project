package com.startup.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
// 브라우저/WebView 로컬 개발을 위한 전역 CORS 설정이다.
// Native Android HTTP 클라이언트는 CORS 대상이 아니지만, Swagger/웹 프론트 테스트를 위해 유지한다.
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns}")
    private String[] allowedOriginPatterns;

    @Value("${app.cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String[] allowedHeaders;

    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(allowCredentials)
                .maxAge(3600);
    }
}
