package com.startup.domain.auth.support;

import com.startup.domain.auth.dto.OAuthLoginRequest;
import com.startup.domain.auth.enums.AuthProvider;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

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
        if (authProperties.getOauth().getKakao().getAppId().isBlank()) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
        }

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
