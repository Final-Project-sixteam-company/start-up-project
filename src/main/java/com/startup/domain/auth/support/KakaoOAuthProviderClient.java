package com.startup.domain.auth.support;

import com.startup.domain.auth.dto.OAuthLoginRequest;
import com.startup.domain.auth.dto.KakaoCodeLoginRequest;
import com.startup.domain.auth.enums.AuthProvider;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KakaoOAuthProviderClient implements OAuthProviderClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthProperties authProperties;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public AuthProvider provider() {
        return AuthProvider.KAKAO;
    }

    @Override
    public OAuthUserProfile verify(OAuthLoginRequest request) {
        String accessToken = normalize(request.accessToken());
        if (accessToken == null) {
            throw new AuthException(AuthErrorCode.OAUTH_TOKEN_REQUIRED);
        }
        ensureTokenVerificationConfigured();

        return verifyAccessToken(accessToken);
    }

    public OAuthUserProfile verifyAuthorizationCode(KakaoCodeLoginRequest request) {
        String authorizationCode = normalize(request.authorizationCode());
        String redirectUri = normalize(request.redirectUri());
        if (authorizationCode == null || redirectUri == null) {
            throw new AuthException(AuthErrorCode.OAUTH_TOKEN_REQUIRED);
        }
        ensureCodeExchangeConfigured();

        String accessToken = exchangeAuthorizationCode(authorizationCode, redirectUri);
        return verifyAccessToken(accessToken);
    }

    private OAuthUserProfile verifyAccessToken(String accessToken) {
        validateTokenApp(accessToken);
        JsonNode root = getUserInfo(accessToken);
        String kakaoUserId = OAuthJsonSupport.textAt(root, "id");
        if (kakaoUserId == null) {
            throw new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }

        return new OAuthUserProfile(
                AuthProvider.KAKAO,
                kakaoUserId,
                OAuthJsonSupport.textAt(root, "kakao_account.email"),
                OAuthJsonSupport.booleanAt(root, "kakao_account.is_email_valid")
                        && OAuthJsonSupport.booleanAt(root, "kakao_account.is_email_verified"),
                OAuthJsonSupport.textAt(root, "kakao_account.profile.nickname"),
                OAuthJsonSupport.textAt(root, "kakao_account.profile.profile_image_url")
        );
    }

    private void ensureTokenVerificationConfigured() {
        if (authProperties.getOauth().getKakao().getAppId().isBlank()) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
        }
    }

    private void ensureCodeExchangeConfigured() {
        ensureTokenVerificationConfigured();
        if (authProperties.getOauth().getKakao().getRestApiKey().isBlank()) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
        }
    }

    private String exchangeAuthorizationCode(String authorizationCode, String redirectUri) {
        try {
            String body = tokenRequestBody(authorizationCode, redirectUri);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(authProperties.getOauth().getKakao().getTokenUri()))
                    .timeout(Duration.ofSeconds(authProperties.getOauth().getTimeoutSeconds()))
                    .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
            }
            JsonNode root = jsonMapper.readTree(response.body());
            String accessToken = OAuthJsonSupport.textAt(root, "access_token");
            if (accessToken == null) {
                throw new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
            }
            return accessToken;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_REQUEST_FAILED);
        }
    }

    private String tokenRequestBody(String authorizationCode, String redirectUri) {
        List<String> params = new ArrayList<>();
        params.add(formParam("grant_type", "authorization_code"));
        params.add(formParam("client_id", authProperties.getOauth().getKakao().getRestApiKey()));
        params.add(formParam("redirect_uri", redirectUri));
        params.add(formParam("code", authorizationCode));
        String clientSecret = normalize(authProperties.getOauth().getKakao().getClientSecret());
        if (clientSecret != null) {
            params.add(formParam("client_secret", clientSecret));
        }
        return String.join("&", params);
    }

    private String formParam(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void validateTokenApp(String accessToken) {
        JsonNode root = sendGet(authProperties.getOauth().getKakao().getAccessTokenInfoUri(), accessToken);
        String appId = OAuthJsonSupport.textAt(root, "app_id");
        if (!authProperties.getOauth().getKakao().getAppId().equals(appId)) {
            throw new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    private JsonNode getUserInfo(String accessToken) {
        return sendGet(authProperties.getOauth().getKakao().getUserInfoUri(), accessToken);
    }

    private JsonNode sendGet(String uri, String accessToken) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(uri))
                    .timeout(Duration.ofSeconds(authProperties.getOauth().getTimeoutSeconds()))
                    .header("Authorization", BEARER_PREFIX + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
            }
            return jsonMapper.readTree(response.body());
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
