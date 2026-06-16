package com.startup.domain.auth.support;

import com.startup.domain.auth.dto.TossLoginRequest;
import com.startup.domain.auth.enums.AuthProvider;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppsInTossOAuthClient implements TossOAuthClient {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SUCCESS = "SUCCESS";
    private static final String INVALID_GRANT = "invalid_grant";
    private static final String GENERATE_TOKEN_PATH = "/api-partner/v1/apps-in-toss/user/oauth2/generate-token";
    private static final String LOGIN_ME_PATH = "/api-partner/v1/apps-in-toss/user/oauth2/login-me";

    private final AuthProperties authProperties;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public OAuthUserProfile verify(TossLoginRequest request) {
        TossToken token = generateToken(request);
        String userKey = loginMe(token.accessToken());
        return new OAuthUserProfile(
                AuthProvider.TOSS,
                userKey,
                null,
                false,
                defaultNickname(userKey),
                null
        );
    }

    private TossToken generateToken(TossLoginRequest request) {
        try {
            String requestBody = jsonMapper.writeValueAsString(Map.of(
                    "authorizationCode", request.authorizationCode().trim(),
                    "referrer", request.referrer().trim()
            ));
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint(GENERATE_TOKEN_PATH))
                    .timeout(Duration.ofSeconds(authProperties.getOauth().getTimeoutSeconds()))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode root = parseBody(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw mapTossError(root);
            }
            JsonNode success = successNode(root);
            String accessToken = requiredText(success, "accessToken");
            String refreshToken = requiredText(success, "refreshToken");
            long expiresIn = requiredLong(success, "expiresIn");
            String scope = OAuthJsonSupport.textAt(success, "scope");
            String tokenType = OAuthJsonSupport.textAt(success, "tokenType");
            return new TossToken(accessToken, refreshToken, expiresIn, scope, tokenType);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_REQUEST_FAILED);
        }
    }

    private String loginMe(String accessToken) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint(LOGIN_ME_PATH))
                    .timeout(Duration.ofSeconds(authProperties.getOauth().getTimeoutSeconds()))
                    .header("Authorization", BEARER_PREFIX + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode root = parseBody(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw mapTossError(root);
            }
            JsonNode success = successNode(root);
            return requiredText(success, "userKey");
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_REQUEST_FAILED);
        }
    }

    private JsonNode parseBody(String body) {
        try {
            return jsonMapper.readTree(body == null ? "{}" : body);
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_REQUEST_FAILED);
        }
    }

    private JsonNode successNode(JsonNode root) {
        String resultType = OAuthJsonSupport.textAt(root, "resultType");
        if (!SUCCESS.equals(resultType)) {
            throw mapTossError(root);
        }
        JsonNode success = root.get("success");
        if (success == null || success.isNull()) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_REQUEST_FAILED);
        }
        return success;
    }

    private AuthException mapTossError(JsonNode root) {
        String errorCode = errorCode(root);
        if (INVALID_GRANT.equals(errorCode)) {
            return new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }
        return new AuthException(AuthErrorCode.OAUTH_PROVIDER_REQUEST_FAILED);
    }

    private String errorCode(JsonNode root) {
        String topLevelError = OAuthJsonSupport.textAt(root, "error");
        if (topLevelError != null) {
            return topLevelError;
        }
        return OAuthJsonSupport.textAt(root, "error.errorCode");
    }

    private String requiredText(JsonNode root, String path) {
        String value = OAuthJsonSupport.textAt(root, path);
        if (value == null) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_REQUEST_FAILED);
        }
        return value;
    }

    private long requiredLong(JsonNode root, String path) {
        String value = requiredText(root, path);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_REQUEST_FAILED);
        }
    }

    private URI endpoint(String path) {
        String baseUrl = authProperties.getOauth().getToss().getApiBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalizedBaseUrl + path);
    }

    private String defaultNickname(String userKey) {
        int start = Math.max(0, userKey.length() - 6);
        return "TossUser-" + userKey.substring(start);
    }

    private record TossToken(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String scope,
            String tokenType
    ) {
    }
}
