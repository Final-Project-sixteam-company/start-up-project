package com.startup.domain.auth.support;

import com.startup.common.auth.AuthenticatedUserPrincipal;
import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.enums.UserRole;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    @Test
    void issueAndParseAccessToken() {
        JwtTokenService tokenService = tokenService("12345678901234567890123456789012");
        User user = User.builder()
                .email("dev@example.com")
                .nickname("Dev User")
                .build();
        ReflectionTestUtils.setField(user, "id", 7L);

        String token = tokenService.issueAccessToken(user);
        AuthenticatedUserPrincipal principal = tokenService.parseAccessToken(token);

        assertThat(principal.userId()).isEqualTo(7L);
        assertThat(principal.email()).isEqualTo("dev@example.com");
        assertThat(principal.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void issueAndParseAccessTokenWithoutEmail() {
        JwtTokenService tokenService = tokenService("12345678901234567890123456789012");
        User user = User.builder()
                .email(null)
                .nickname("OAuth User")
                .build();
        ReflectionTestUtils.setField(user, "id", 8L);

        String token = tokenService.issueAccessToken(user);
        AuthenticatedUserPrincipal principal = tokenService.parseAccessToken(token);

        assertThat(principal.userId()).isEqualTo(8L);
        assertThat(principal.email()).isNull();
        assertThat(principal.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void issueAccessTokenFailsWhenSecretIsNotConfigured() {
        JwtTokenService tokenService = tokenService("");
        User user = User.builder()
                .email("dev@example.com")
                .nickname("Dev User")
                .build();
        ReflectionTestUtils.setField(user, "id", 7L);

        assertThatThrownBy(() -> tokenService.issueAccessToken(user))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.JWT_SECRET_NOT_CONFIGURED);
    }

    @Test
    void parseAccessTokenRejectsTamperedToken() {
        JwtTokenService tokenService = tokenService("12345678901234567890123456789012");
        User user = User.builder()
                .email("dev@example.com")
                .nickname("Dev User")
                .build();
        ReflectionTestUtils.setField(user, "id", 7L);
        String token = tokenService.issueAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> tokenService.parseAccessToken(tampered))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_TOKEN);
    }

    @Test
    void hashRefreshTokenUsesSha256Hex() {
        JwtTokenService tokenService = tokenService("12345678901234567890123456789012");

        String hash = tokenService.hashRefreshToken("refresh-token");

        assertThat(hash).hasSize(64);
        assertThat(hash).isNotEqualTo("refresh-token");
        assertThat(hash).isEqualTo(tokenService.hashRefreshToken("refresh-token"));
    }

    private JwtTokenService tokenService(String secret) {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getJwt().setSecret(secret);
        return new JwtTokenService(authProperties, JsonMapper.builder().build());
    }
}
