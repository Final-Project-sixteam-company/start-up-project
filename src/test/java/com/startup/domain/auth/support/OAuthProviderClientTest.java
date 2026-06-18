package com.startup.domain.auth.support;

import com.startup.domain.auth.dto.TossLoginRequest;
import com.startup.domain.auth.dto.OAuthLoginRequest;
import com.startup.domain.auth.enums.AuthProvider;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthProviderClientTest {

    @Test
    void googleRequiresIdToken() {
        GoogleOAuthProviderClient client = new GoogleOAuthProviderClient(properties(), JsonMapper.builder().build());

        assertThatThrownBy(() -> client.verify(new OAuthLoginRequest(AuthProvider.GOOGLE, null, null, null)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_TOKEN_REQUIRED);
    }

    @Test
    void googleRequiresConfiguredClientIds() {
        GoogleOAuthProviderClient client = new GoogleOAuthProviderClient(properties(), JsonMapper.builder().build());

        assertThatThrownBy(() -> client.verify(new OAuthLoginRequest(AuthProvider.GOOGLE, "id-token", null, null)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }

    @Test
    void kakaoRequiresAccessToken() {
        KakaoOAuthProviderClient client = new KakaoOAuthProviderClient(properties(), JsonMapper.builder().build());

        assertThatThrownBy(() -> client.verify(new OAuthLoginRequest(AuthProvider.KAKAO, null, null, null)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_TOKEN_REQUIRED);
    }

    @Test
    void kakaoRequiresConfiguredAppId() {
        KakaoOAuthProviderClient client = new KakaoOAuthProviderClient(properties(), JsonMapper.builder().build());

        assertThatThrownBy(() -> client.verify(new OAuthLoginRequest(AuthProvider.KAKAO, null, "access-token", null)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }

    @Test
    void kakaoAuthorizationCodeExchangeThenVerifiesUser() throws Exception {
        AtomicReference<String> tokenRequestBody = new AtomicReference<>();
        AtomicReference<String> tokenInfoAuthorization = new AtomicReference<>();
        AtomicReference<String> userInfoAuthorization = new AtomicReference<>();
        HttpServer server = startKakaoServer(
                200,
                """
                        {
                          "access_token": "kakao-access",
                          "token_type": "bearer",
                          "expires_in": 21599
                        }
                        """,
                200,
                """
                        {
                          "id": 123456,
                          "app_id": 1485204,
                          "expires_in": 21599
                        }
                        """,
                200,
                """
                        {
                          "id": 123456,
                          "kakao_account": {
                            "email": "kakao@example.com",
                            "is_email_valid": true,
                            "is_email_verified": true,
                            "profile": {
                              "nickname": "카카오 탐정",
                              "profile_image_url": "https://example.com/profile.png"
                            }
                          }
                        }
                        """,
                tokenRequestBody,
                tokenInfoAuthorization,
                userInfoAuthorization
        );

        try {
            KakaoOAuthProviderClient client = new KakaoOAuthProviderClient(kakaoProperties(server), JsonMapper.builder().build());

            OAuthUserProfile profile = client.verifyAuthorizationCode(
                    new com.startup.domain.auth.dto.KakaoCodeLoginRequest(
                            "auth-code",
                            "https://www.clueroom.xyz",
                            "web-device"
                    )
            );

            assertThat(tokenRequestBody.get())
                    .contains("grant_type=authorization_code")
                    .contains("client_id=kakao-rest-key")
                    .contains("redirect_uri=https%3A%2F%2Fwww.clueroom.xyz")
                    .contains("code=auth-code");
            assertThat(tokenInfoAuthorization.get()).isEqualTo("Bearer kakao-access");
            assertThat(userInfoAuthorization.get()).isEqualTo("Bearer kakao-access");
            assertThat(profile.provider()).isEqualTo(AuthProvider.KAKAO);
            assertThat(profile.providerUserId()).isEqualTo("123456");
            assertThat(profile.email()).isEqualTo("kakao@example.com");
            assertThat(profile.emailVerified()).isTrue();
            assertThat(profile.nickname()).isEqualTo("카카오 탐정");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void kakaoAuthorizationCodeRequiresRestApiKey() {
        AuthProperties authProperties = properties();
        authProperties.getOauth().getKakao().setAppId("1485204");
        KakaoOAuthProviderClient client = new KakaoOAuthProviderClient(authProperties, JsonMapper.builder().build());

        assertThatThrownBy(() -> client.verifyAuthorizationCode(
                new com.startup.domain.auth.dto.KakaoCodeLoginRequest("auth-code", "https://www.clueroom.xyz", null)))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }

    @Test
    void appsInTossParsesNumericExpiresInAndNumericUserKey() throws Exception {
        AtomicReference<String> tokenRequestBody = new AtomicReference<>();
        AtomicReference<String> loginMeAuthorization = new AtomicReference<>();
        HttpServer server = startTossServer(
                200,
                """
                        {
                          "resultType": "SUCCESS",
                          "success": {
                            "accessToken": "toss-access",
                            "refreshToken": "toss-refresh",
                            "scope": "user_key",
                            "tokenType": "Bearer",
                            "expiresIn": 3599
                          }
                        }
                        """,
                200,
                """
                        {
                          "resultType": "SUCCESS",
                          "success": {
                            "userKey": 443731104,
                            "scope": "user_key",
                            "agreedTerms": []
                          }
                        }
                        """,
                tokenRequestBody,
                loginMeAuthorization
        );

        try {
            AppsInTossOAuthClient client = new AppsInTossOAuthClient(tossProperties(server), JsonMapper.builder().build());

            OAuthUserProfile profile = client.verify(new TossLoginRequest("auth-code", "SANDBOX", "device"));

            assertThat(tokenRequestBody.get()).contains("authorizationCode", "auth-code", "referrer", "SANDBOX");
            assertThat(loginMeAuthorization.get()).isEqualTo("Bearer toss-access");
            assertThat(profile.provider()).isEqualTo(AuthProvider.TOSS);
            assertThat(profile.providerUserId()).isEqualTo("443731104");
            assertThat(profile.email()).isNull();
            assertThat(profile.emailVerified()).isFalse();
            assertThat(profile.nickname()).isEqualTo("TossUser-731104");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void appsInTossParsesStringExpiresIn() throws Exception {
        HttpServer server = startTossServer(
                200,
                """
                        {
                          "resultType": "SUCCESS",
                          "success": {
                            "accessToken": "toss-access",
                            "refreshToken": "toss-refresh",
                            "scope": "user_key",
                            "tokenType": "Bearer",
                            "expiresIn": "3599"
                          }
                        }
                        """,
                200,
                """
                        {
                          "resultType": "SUCCESS",
                          "success": {
                            "userKey": "short",
                            "scope": "user_key",
                            "agreedTerms": []
                          }
                        }
                        """,
                new AtomicReference<>(),
                new AtomicReference<>()
        );

        try {
            AppsInTossOAuthClient client = new AppsInTossOAuthClient(tossProperties(server), JsonMapper.builder().build());

            OAuthUserProfile profile = client.verify(new TossLoginRequest("auth-code", "DEFAULT", "device"));

            assertThat(profile.providerUserId()).isEqualTo("short");
            assertThat(profile.nickname()).isEqualTo("TossUser-short");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void appsInTossMapsInvalidGrant() throws Exception {
        HttpServer server = startTossServer(
                200,
                """
                        {
                          "error": "invalid_grant"
                        }
                        """,
                200,
                """
                        {
                          "resultType": "SUCCESS",
                          "success": {
                            "userKey": 443731104
                          }
                        }
                        """,
                new AtomicReference<>(),
                new AtomicReference<>()
        );

        try {
            AppsInTossOAuthClient client = new AppsInTossOAuthClient(tossProperties(server), JsonMapper.builder().build());

            assertThatThrownBy(() -> client.verify(new TossLoginRequest("expired-code", "DEFAULT", "device")))
                    .isInstanceOf(AuthException.class)
                    .extracting(e -> ((AuthException) e).getErrorCode())
                    .isEqualTo(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void appsInTossRealEndpointRequiresMtlsConfiguration() {
        AppsInTossOAuthClient client = new AppsInTossOAuthClient(properties(), JsonMapper.builder().build());

        assertThatThrownBy(() -> client.verify(new TossLoginRequest("auth-code", "DEFAULT", "device")))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }

    @Test
    void appsInTossRealEndpointRequiresBothMtlsPaths() {
        AuthProperties authProperties = properties();
        authProperties.getOauth().getToss().setMtlsCertPath("/opt/clueroom/secrets/toss/client.crt");
        AppsInTossOAuthClient client = new AppsInTossOAuthClient(authProperties, JsonMapper.builder().build());

        assertThatThrownBy(() -> client.verify(new TossLoginRequest("auth-code", "DEFAULT", "device")))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }

    @Test
    void appsInTossRejectsInvalidMtlsFiles() {
        AuthProperties authProperties = properties();
        authProperties.getOauth().getToss().setMtlsCertPath("/opt/clueroom/secrets/toss/missing-client.crt");
        authProperties.getOauth().getToss().setMtlsKeyPath("/opt/clueroom/secrets/toss/missing-client.key");

        assertThatThrownBy(() -> new AppsInTossOAuthClient(authProperties, JsonMapper.builder().build()))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }

    private AuthProperties properties() {
        return new AuthProperties();
    }

    private AuthProperties tossProperties(HttpServer server) {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getOauth().getToss().setApiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        return authProperties;
    }

    private AuthProperties kakaoProperties(HttpServer server) {
        AuthProperties authProperties = new AuthProperties();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        authProperties.getOauth().getKakao().setAppId("1485204");
        authProperties.getOauth().getKakao().setRestApiKey("kakao-rest-key");
        authProperties.getOauth().getKakao().setTokenUri(baseUrl + "/oauth/token");
        authProperties.getOauth().getKakao().setAccessTokenInfoUri(baseUrl + "/v1/user/access_token_info");
        authProperties.getOauth().getKakao().setUserInfoUri(baseUrl + "/v2/user/me");
        return authProperties;
    }

    private HttpServer startKakaoServer(
            int tokenStatus,
            String tokenResponse,
            int tokenInfoStatus,
            String tokenInfoResponse,
            int userInfoStatus,
            String userInfoResponse,
            AtomicReference<String> tokenRequestBody,
            AtomicReference<String> tokenInfoAuthorization,
            AtomicReference<String> userInfoAuthorization
    ) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth/token", exchange -> {
            tokenRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respondJson(exchange, tokenStatus, tokenResponse);
        });
        server.createContext("/v1/user/access_token_info", exchange -> {
            tokenInfoAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respondJson(exchange, tokenInfoStatus, tokenInfoResponse);
        });
        server.createContext("/v2/user/me", exchange -> {
            userInfoAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respondJson(exchange, userInfoStatus, userInfoResponse);
        });
        server.start();
        return server;
    }

    private HttpServer startTossServer(
            int tokenStatus,
            String tokenResponse,
            int loginMeStatus,
            String loginMeResponse,
            AtomicReference<String> tokenRequestBody,
            AtomicReference<String> loginMeAuthorization
    ) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api-partner/v1/apps-in-toss/user/oauth2/generate-token", exchange -> {
            tokenRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respondJson(exchange, tokenStatus, tokenResponse);
        });
        server.createContext("/api-partner/v1/apps-in-toss/user/oauth2/login-me", exchange -> {
            loginMeAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respondJson(exchange, loginMeStatus, loginMeResponse);
        });
        server.start();
        return server;
    }

    private void respondJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
