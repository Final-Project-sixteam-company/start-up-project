package com.startup.domain.auth.controller;

import com.startup.common.dto.ApiResponse;
import com.startup.domain.auth.dto.AuthMeResponse;
import com.startup.domain.auth.dto.AuthTokenResponse;
import com.startup.domain.auth.dto.DevLoginRequest;
import com.startup.domain.auth.dto.KakaoCodeLoginRequest;
import com.startup.domain.auth.dto.LogoutRequest;
import com.startup.domain.auth.dto.OAuthLoginRequest;
import com.startup.domain.auth.dto.TokenRefreshRequest;
import com.startup.domain.auth.dto.TossLoginRequest;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import com.startup.domain.auth.service.AuthService;
import com.startup.domain.auth.support.AuthCookieSupport;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieSupport authCookieSupport;

    @PostMapping("/dev")
    public ApiResponse<AuthTokenResponse> devLogin(
            @Valid @RequestBody DevLoginRequest request,
            HttpServletResponse response
    ) {
        AuthTokenResponse tokenResponse = authService.devLogin(request);
        authCookieSupport.addRefreshCookie(response, tokenResponse.refreshToken());
        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/oauth")
    public ApiResponse<AuthTokenResponse> oauthLogin(
            @Valid @RequestBody OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        AuthTokenResponse tokenResponse = authService.oauthLogin(request);
        authCookieSupport.addRefreshCookie(response, tokenResponse.refreshToken());
        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/oauth/kakao/code")
    public ApiResponse<AuthTokenResponse> kakaoCodeLogin(
            @Valid @RequestBody KakaoCodeLoginRequest request,
            HttpServletResponse response
    ) {
        AuthTokenResponse tokenResponse = authService.kakaoCodeLogin(request);
        authCookieSupport.addRefreshCookie(response, tokenResponse.refreshToken());
        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/toss")
    public ApiResponse<AuthTokenResponse> tossLogin(
            @Valid @RequestBody TossLoginRequest request,
            HttpServletResponse response
    ) {
        AuthTokenResponse tokenResponse = authService.tossLogin(request);
        authCookieSupport.addRefreshCookie(response, tokenResponse.refreshToken());
        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(
            @Valid @RequestBody(required = false) TokenRefreshRequest request,
            @CookieValue(name = "${clueroom.auth.refresh-cookie.name:clueroom_refresh_token}", required = false)
            String cookieRefreshToken,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(request == null ? null : request.refreshToken(), cookieRefreshToken);
        String deviceId = request == null ? null : request.deviceId();
        AuthTokenResponse tokenResponse = authService.refresh(refreshToken, deviceId);
        authCookieSupport.addRefreshCookie(response, tokenResponse.refreshToken());
        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody(required = false) LogoutRequest request,
            @CookieValue(name = "${clueroom.auth.refresh-cookie.name:clueroom_refresh_token}", required = false)
            String cookieRefreshToken,
            HttpServletResponse response
    ) {
        String refreshToken = firstText(request == null ? null : request.refreshToken(), cookieRefreshToken);
        if (StringUtils.hasText(refreshToken)) {
            authService.logout(refreshToken);
        }
        authCookieSupport.clearRefreshCookie(response);
        return ApiResponse.empty();
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me() {
        return ApiResponse.success(authService.me());
    }

    private String resolveRefreshToken(String bodyRefreshToken, String cookieRefreshToken) {
        String refreshToken = firstText(bodyRefreshToken, cookieRefreshToken);
        if (!StringUtils.hasText(refreshToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        return refreshToken;
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : null;
    }
}
