package com.startup.domain.auth.support;

import com.startup.domain.auth.dto.TossLoginRequest;
import com.startup.domain.auth.enums.AuthProvider;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class AppsInTossOAuthClient implements TossOAuthClient {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SUCCESS = "SUCCESS";
    private static final String INVALID_GRANT = "invalid_grant";
    private static final String REAL_TOSS_API_HOST = "apps-in-toss-api.toss.im";
    private static final String GENERATE_TOKEN_PATH = "/api-partner/v1/apps-in-toss/user/oauth2/generate-token";
    private static final String LOGIN_ME_PATH = "/api-partner/v1/apps-in-toss/user/oauth2/login-me";
    private static final char[] EMPTY_PASSWORD = new char[0];

    private final AuthProperties authProperties;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;

    public AppsInTossOAuthClient(AuthProperties authProperties, JsonMapper jsonMapper) {
        this.authProperties = authProperties;
        this.jsonMapper = jsonMapper;
        this.httpClient = buildHttpClient(authProperties.getOauth().getToss());
    }

    @Override
    public OAuthUserProfile verify(TossLoginRequest request) {
        requireMtlsForRealEndpoint();
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

    private HttpClient buildHttpClient(AuthProperties.Toss tossProperties) {
        if (!tossProperties.isMtlsConfigured()) {
            return HttpClient.newHttpClient();
        }

        try {
            SSLContext sslContext = buildMtlsSslContext(tossProperties);
            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
        }
    }

    private SSLContext buildMtlsSslContext(AuthProperties.Toss tossProperties) throws Exception {
        Certificate[] certificateChain = loadCertificateChain(tossProperties.getMtlsCertPath());
        PrivateKey privateKey = loadPkcs8PrivateKey(tossProperties.getMtlsKeyPath());

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, EMPTY_PASSWORD);
        keyStore.setKeyEntry("apps-in-toss-client", privateKey, EMPTY_PASSWORD, certificateChain);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, EMPTY_PASSWORD);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    private Certificate[] loadCertificateChain(String certPath) throws Exception {
        try (var inputStream = Files.newInputStream(Path.of(certPath))) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(inputStream);
            if (certificates.isEmpty()) {
                throw new GeneralSecurityException("Empty certificate chain");
            }
            return certificates.toArray(Certificate[]::new);
        }
    }

    private PrivateKey loadPkcs8PrivateKey(String keyPath) throws Exception {
        String pem = Files.readString(Path.of(keyPath), StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);

        for (String algorithm : List.of("RSA", "EC")) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
            } catch (GeneralSecurityException ignored) {
                // Try the next common Toss client-key algorithm.
            }
        }
        throw new GeneralSecurityException("Unsupported private key algorithm");
    }

    private void requireMtlsForRealEndpoint() {
        AuthProperties.Toss tossProperties = authProperties.getOauth().getToss();
        if (isRealTossEndpoint(tossProperties.getApiBaseUrl()) && !tossProperties.isMtlsConfigured()) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
        }
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

    private boolean isRealTossEndpoint(String apiBaseUrl) {
        try {
            URI uri = URI.create(apiBaseUrl);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && REAL_TOSS_API_HOST.equalsIgnoreCase(uri.getHost());
        } catch (IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
        }
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
