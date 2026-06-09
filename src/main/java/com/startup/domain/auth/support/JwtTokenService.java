package com.startup.domain.auth.support;

import com.startup.common.auth.AuthenticatedUserPrincipal;
import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final AuthProperties authProperties;
    private final JsonMapper jsonMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenService(AuthProperties authProperties, JsonMapper jsonMapper) {
        this.authProperties = authProperties;
        this.jsonMapper = jsonMapper;
    }

    public String issueAccessToken(User user) {
        ensureJwtSecretConfigured();

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenExpiresInSeconds());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", authProperties.getJwt().getIssuer());
        claims.put("sub", String.valueOf(user.getId()));
        claims.put("uid", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("typ", TOKEN_TYPE_ACCESS);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(claims);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public AuthenticatedUserPrincipal parseAccessToken(String token) {
        ensureJwtSecretConfigured();
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            }

            String unsignedToken = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            }

            Map<String, Object> claims = jsonMapper.readValue(base64UrlDecode(parts[1]), MAP_TYPE);
            if (!TOKEN_TYPE_ACCESS.equals(asString(claims.get("typ")))) {
                throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            }
            if (!authProperties.getJwt().getIssuer().equals(asString(claims.get("iss")))) {
                throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            }
            long expiresAt = asLong(claims.get("exp"));
            if (Instant.now().getEpochSecond() >= expiresAt) {
                throw new AuthException(AuthErrorCode.INVALID_TOKEN);
            }

            Long userId = Long.valueOf(asString(claims.get("sub")));
            String email = asNullableString(claims.get("email"));
            return new AuthenticatedUserPrincipal(
                    userId,
                    email,
                    com.startup.domain.auth.enums.UserRole.valueOf(asString(claims.get("role")))
            );
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    public long accessTokenExpiresInSeconds() {
        return authProperties.getJwt().getAccessTokenTtlSeconds();
    }

    public LocalDateTime refreshTokenExpiresAt() {
        return LocalDateTime.now().plusDays(authProperties.getJwt().getRefreshTokenTtlDays());
    }

    private void ensureJwtSecretConfigured() {
        if (!authProperties.isJwtSecretConfigured()) {
            throw new AuthException(AuthErrorCode.JWT_SECRET_NOT_CONFIGURED);
        }
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(jsonMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private byte[] base64UrlDecode(String encoded) {
        return Base64.getUrlDecoder().decode(encoded);
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private String asString(Object value) {
        if (value == null) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        return String.valueOf(value);
    }

    private String asNullableString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(asString(value));
    }
}
