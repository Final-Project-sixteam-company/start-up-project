package com.startup.domain.auth.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "clueroom.auth")
public class AuthProperties {

    private static final int MIN_HS256_SECRET_LENGTH = 32;

    private boolean mockFallbackEnabled = true;
    private boolean devLoginEnabled = false;
    private boolean requireAuthentication = false;
    private Jwt jwt = new Jwt();
    private OAuth oauth = new OAuth();

    public boolean isMockFallbackEnabled() {
        return mockFallbackEnabled;
    }

    public void setMockFallbackEnabled(boolean mockFallbackEnabled) {
        this.mockFallbackEnabled = mockFallbackEnabled;
    }

    public boolean isDevLoginEnabled() {
        return devLoginEnabled;
    }

    public void setDevLoginEnabled(boolean devLoginEnabled) {
        this.devLoginEnabled = devLoginEnabled;
    }

    public boolean isRequireAuthentication() {
        return requireAuthentication;
    }

    public void setRequireAuthentication(boolean requireAuthentication) {
        this.requireAuthentication = requireAuthentication;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt == null ? new Jwt() : jwt;
    }

    public OAuth getOauth() {
        return oauth;
    }

    public void setOauth(OAuth oauth) {
        this.oauth = oauth == null ? new OAuth() : oauth;
    }

    public boolean isJwtSecretConfigured() {
        String secret = jwt.getSecret();
        return secret != null && secret.length() >= MIN_HS256_SECRET_LENGTH;
    }

    public static class Jwt {
        private String issuer = "https://api.clueroom.xyz";
        private String secret = "";
        private long accessTokenTtlSeconds = 1800;
        private int refreshTokenTtlDays = 30;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = (issuer == null || issuer.isBlank()) ? "https://api.clueroom.xyz" : issuer.trim();
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret == null ? "" : secret;
        }

        public long getAccessTokenTtlSeconds() {
            return accessTokenTtlSeconds;
        }

        public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
            this.accessTokenTtlSeconds = Math.max(60, accessTokenTtlSeconds);
        }

        public int getRefreshTokenTtlDays() {
            return refreshTokenTtlDays;
        }

        public void setRefreshTokenTtlDays(int refreshTokenTtlDays) {
            this.refreshTokenTtlDays = Math.max(1, refreshTokenTtlDays);
        }
    }

    public static class OAuth {
        private int timeoutSeconds = 5;
        private Google google = new Google();
        private Kakao kakao = new Kakao();

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = Math.max(1, timeoutSeconds);
        }

        public Google getGoogle() {
            return google;
        }

        public void setGoogle(Google google) {
            this.google = google == null ? new Google() : google;
        }

        public Kakao getKakao() {
            return kakao;
        }

        public void setKakao(Kakao kakao) {
            this.kakao = kakao == null ? new Kakao() : kakao;
        }
    }

    public static class Google {
        private String clientIds = "";
        private String tokenInfoUri = "https://oauth2.googleapis.com/tokeninfo";

        public String getClientIds() {
            return clientIds;
        }

        public void setClientIds(String clientIds) {
            this.clientIds = clientIds == null ? "" : clientIds;
        }

        public String getTokenInfoUri() {
            return tokenInfoUri;
        }

        public void setTokenInfoUri(String tokenInfoUri) {
            this.tokenInfoUri = (tokenInfoUri == null || tokenInfoUri.isBlank())
                    ? "https://oauth2.googleapis.com/tokeninfo"
                    : tokenInfoUri.trim();
        }

        public List<String> clientIdList() {
            return Arrays.stream(clientIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
    }

    public static class Kakao {
        private String appId = "";
        private String accessTokenInfoUri = "https://kapi.kakao.com/v1/user/access_token_info";
        private String userInfoUri = "https://kapi.kakao.com/v2/user/me";

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId == null ? "" : appId.trim();
        }

        public String getAccessTokenInfoUri() {
            return accessTokenInfoUri;
        }

        public void setAccessTokenInfoUri(String accessTokenInfoUri) {
            this.accessTokenInfoUri = (accessTokenInfoUri == null || accessTokenInfoUri.isBlank())
                    ? "https://kapi.kakao.com/v1/user/access_token_info"
                    : accessTokenInfoUri.trim();
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = (userInfoUri == null || userInfoUri.isBlank())
                    ? "https://kapi.kakao.com/v2/user/me"
                    : userInfoUri.trim();
        }
    }
}
