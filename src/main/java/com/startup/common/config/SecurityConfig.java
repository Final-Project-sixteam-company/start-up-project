package com.startup.common.config;

import com.startup.common.auth.JwtAuthenticationFilter;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.ErrorResponse;
import com.startup.common.error.CommonErrorCode;
import com.startup.common.error.ErrorCode;
import com.startup.domain.auth.support.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthProperties authProperties;
    private final JsonMapper jsonMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(this::authorizeRequests)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void authorizeRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth
    ) {
        if (!authProperties.isRequireAuthentication()) {
            /**
             * Legacy compatibility mode:
             * 인증(Token) 없이 들어온 요청도 Security를 통과시켜, 이후 MockUserProvider를 통해
             * Mock User(임시 사용자) 자격을 부여받도록 허용하는 의도된 Trade-off입니다.
             */
            auth.anyRequest().permitAll();
            return;
        }

        auth
                .requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/dev", "/api/auth/oauth", "/api/auth/toss",
                        "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/scenarios/me", "/api/scenarios/bookmarked").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/scenarios", "/api/scenarios/{scenarioId}", "/api/scenarios/{scenarioId}/reviews").permitAll()
                .requestMatchers("/api/play-sessions/**").authenticated()
                .requestMatchers("/api/device-tokens/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/scenarios/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/scenarios/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/scenarios/{scenarioId}/validation-result").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/ai/scenarios/{scenarioId}/validate").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                writeErrorResponse(response, request, CommonErrorCode.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED.getMessage());
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeErrorResponse(response, request, CommonErrorCode.ACCESS_DENIED, CommonErrorCode.ACCESS_DENIED.getMessage());
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            ErrorCode errorCode,
            String message
    ) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .code(errorCode.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(ApiResponse.fail(errorResponse)));
    }
}
