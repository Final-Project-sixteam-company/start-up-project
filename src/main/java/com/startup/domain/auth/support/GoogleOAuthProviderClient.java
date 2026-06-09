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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GoogleOAuthProviderClient implements OAuthProviderClient {

    private final AuthProperties authProperties;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserProfile verify(OAuthLoginRequest request) {
        String idToken = normalize(request.idToken());
        if (idToken == null) {
            throw new AuthException(AuthErrorCode.OAUTH_TOKEN_REQUIRED);
        }

        List<String> clientIds = authProperties.getOauth().getGoogle().clientIdList();
        if (clientIds.isEmpty()) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
        }

        JsonNode root = getTokenInfo(idToken);
        String audience = OAuthJsonSupport.textAt(root, "aud");
        if (!clientIds.contains(audience)) {
            throw new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }

        String subject = OAuthJsonSupport.textAt(root, "sub");
        if (subject == null) {
            throw new AuthException(AuthErrorCode.OAUTH_TOKEN_VERIFICATION_FAILED);
        }

        return new OAuthUserProfile(
                AuthProvider.GOOGLE,
                subject,
                OAuthJsonSupport.textAt(root, "email"),
                OAuthJsonSupport.textAt(root, "name"),
                OAuthJsonSupport.textAt(root, "picture")
        );
    }

    private JsonNode getTokenInfo(String idToken) {
        try {
            String uri = authProperties.getOauth().getGoogle().getTokenInfoUri()
                    + "?id_token=" + URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(uri))
                    .timeout(Duration.ofSeconds(authProperties.getOauth().getTimeoutSeconds()))
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
