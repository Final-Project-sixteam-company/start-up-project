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
    private RefreshCookie refreshCookie = new RefreshCookie();
    private OAuth oauth = new OAuth();
    private AdminSeed adminSeed = new AdminSeed();
    private QaSeed qaSeed = new QaSeed();

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

    public RefreshCookie getRefreshCookie() {
        return refreshCookie;
    }

    public void setRefreshCookie(RefreshCookie refreshCookie) {
        this.refreshCookie = refreshCookie == null ? new RefreshCookie() : refreshCookie;
    }

    public OAuth getOauth() {
        return oauth;
    }

    public void setOauth(OAuth oauth) {
        this.oauth = oauth == null ? new OAuth() : oauth;
    }

    public AdminSeed getAdminSeed() {
        return adminSeed;
    }

    public void setAdminSeed(AdminSeed adminSeed) {
        this.adminSeed = adminSeed == null ? new AdminSeed() : adminSeed;
    }

    public QaSeed getQaSeed() {
        return qaSeed;
    }

    public void setQaSeed(QaSeed qaSeed) {
        this.qaSeed = qaSeed == null ? new QaSeed() : qaSeed;
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

    public static class RefreshCookie {
        private boolean enabled = true;
        private String name = "clueroom_refresh_token";
        private String path = "/api/auth";
        private String domain = "";
        private boolean secure = true;
        private String sameSite = "Lax";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = (name == null || name.isBlank()) ? "clueroom_refresh_token" : name.trim();
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = (path == null || path.isBlank()) ? "/api/auth" : path.trim();
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain == null ? "" : domain.trim();
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = (sameSite == null || sameSite.isBlank()) ? "Lax" : sameSite.trim();
        }
    }

    public static class OAuth {
        private int timeoutSeconds = 5;
        private Google google = new Google();
        private Kakao kakao = new Kakao();
        private Toss toss = new Toss();

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

        public Toss getToss() {
            return toss;
        }

        public void setToss(Toss toss) {
            this.toss = toss == null ? new Toss() : toss;
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
        private String restApiKey = "";
        private String clientSecret = "";
        private String tokenUri = "https://kauth.kakao.com/oauth/token";
        private String accessTokenInfoUri = "https://kapi.kakao.com/v1/user/access_token_info";
        private String userInfoUri = "https://kapi.kakao.com/v2/user/me";

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId == null ? "" : appId.trim();
        }

        public String getRestApiKey() {
            return restApiKey;
        }

        public void setRestApiKey(String restApiKey) {
            this.restApiKey = restApiKey == null ? "" : restApiKey.trim();
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = (tokenUri == null || tokenUri.isBlank())
                    ? "https://kauth.kakao.com/oauth/token"
                    : tokenUri.trim();
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

    public static class Toss {
        private String apiBaseUrl = "https://apps-in-toss-api.toss.im";
        private String mtlsCertPath = "";
        private String mtlsKeyPath = "";

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = (apiBaseUrl == null || apiBaseUrl.isBlank())
                    ? "https://apps-in-toss-api.toss.im"
                    : apiBaseUrl.trim();
        }

        public String getMtlsCertPath() {
            return mtlsCertPath;
        }

        public void setMtlsCertPath(String mtlsCertPath) {
            this.mtlsCertPath = mtlsCertPath == null ? "" : mtlsCertPath.trim();
        }

        public String getMtlsKeyPath() {
            return mtlsKeyPath;
        }

        public void setMtlsKeyPath(String mtlsKeyPath) {
            this.mtlsKeyPath = mtlsKeyPath == null ? "" : mtlsKeyPath.trim();
        }

        public boolean isMtlsConfigured() {
            return !mtlsCertPath.isBlank() && !mtlsKeyPath.isBlank();
        }
    }

    public static class AdminSeed {
        private boolean enabled = false;
        private String email = "";
        private String nickname = "ClueRoom Admin";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email == null ? "" : email.trim();
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = (nickname == null || nickname.isBlank()) ? "ClueRoom Admin" : nickname.trim();
        }
    }

    public static class QaSeed {
        private boolean enabled = false;
        private String email = "";
        private String nickname = "ClueRoom QA";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email == null ? "" : email.trim();
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = (nickname == null || nickname.isBlank()) ? "ClueRoom QA" : nickname.trim();
        }
    }
}
