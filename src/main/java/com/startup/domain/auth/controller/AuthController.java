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
import com.startup.domain.auth.service.AuthService;
import jakarta.validation.Valid;
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

    @PostMapping("/dev")
    public ApiResponse<AuthTokenResponse> devLogin(@Valid @RequestBody DevLoginRequest request) {
        return ApiResponse.success(authService.devLogin(request));
    }

    @PostMapping("/oauth")
    public ApiResponse<AuthTokenResponse> oauthLogin(@Valid @RequestBody OAuthLoginRequest request) {
        return ApiResponse.success(authService.oauthLogin(request));
    }

    @PostMapping("/oauth/kakao/code")
    public ApiResponse<AuthTokenResponse> kakaoCodeLogin(@Valid @RequestBody KakaoCodeLoginRequest request) {
        return ApiResponse.success(authService.kakaoCodeLogin(request));
    }

    @PostMapping("/toss")
    public ApiResponse<AuthTokenResponse> tossLogin(@Valid @RequestBody TossLoginRequest request) {
        return ApiResponse.success(authService.tossLogin(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.empty();
    }

    @GetMapping("/me")
    public ApiResponse<AuthMeResponse> me() {
        return ApiResponse.success(authService.me());
    }
}
