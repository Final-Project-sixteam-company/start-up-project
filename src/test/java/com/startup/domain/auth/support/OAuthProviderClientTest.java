package com.startup.domain.auth.support;

import com.startup.domain.auth.dto.OAuthLoginRequest;
import com.startup.domain.auth.enums.AuthProvider;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

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

    private AuthProperties properties() {
        return new AuthProperties();
    }
}
