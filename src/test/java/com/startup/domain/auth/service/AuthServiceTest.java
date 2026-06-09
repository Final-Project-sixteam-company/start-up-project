package com.startup.domain.auth.service;

import com.startup.common.auth.CurrentUserProvider;
import com.startup.domain.auth.dto.AuthTokenResponse;
import com.startup.domain.auth.dto.OAuthLoginRequest;
import com.startup.domain.auth.dto.TokenRefreshRequest;
import com.startup.domain.auth.entity.AuthRefreshToken;
import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.entity.UserOAuthAccount;
import com.startup.domain.auth.enums.AuthProvider;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import com.startup.domain.auth.repository.AuthRefreshTokenRepository;
import com.startup.domain.auth.repository.UserOAuthAccountRepository;
import com.startup.domain.auth.repository.UserRepository;
import com.startup.domain.auth.support.AuthProperties;
import com.startup.domain.auth.support.JwtTokenService;
import com.startup.domain.auth.support.OAuthProviderClient;
import com.startup.domain.auth.support.OAuthUserProfile;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void oauthLoginCreatesUserLinksProviderAndIssuesTokens() {
        AuthProperties authProperties = properties();
        JwtTokenService jwtTokenService = new JwtTokenService(authProperties, JsonMapper.builder().build());
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOAuthAccountRepository accountRepository = mock(UserOAuthAccountRepository.class);
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        OAuthProviderClient providerClient = fakeGoogleClient();
        AuthService authService = new AuthService(
                authProperties,
                jwtTokenService,
                currentUserProvider,
                userRepository,
                accountRepository,
                refreshTokenRepository,
                transactionManager(),
                List.of(providerClient)
        );

        when(accountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 10L);
            return user;
        });
        when(accountRepository.findByUserIdAndProvider(10L, AuthProvider.GOOGLE)).thenReturn(Optional.empty());
        when(accountRepository.saveAndFlush(any(UserOAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(AuthRefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokenResponse response = authService.oauthLogin(
                new OAuthLoginRequest(AuthProvider.GOOGLE, "id-token", null, "android")
        );

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().userId()).isEqualTo(10L);
        assertThat(response.user().email()).isEqualTo("oauth@example.com");
        verify(accountRepository).saveAndFlush(any(UserOAuthAccount.class));
        verify(refreshTokenRepository).save(any(AuthRefreshToken.class));
    }

    private AuthProperties properties() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getJwt().setSecret("12345678901234567890123456789012");
        return authProperties;
    }

    private OAuthProviderClient fakeGoogleClient() {
        return new OAuthProviderClient() {
            @Override
            public AuthProvider provider() {
                return AuthProvider.GOOGLE;
            }

            @Override
            public OAuthUserProfile verify(OAuthLoginRequest request) {
                return new OAuthUserProfile(
                        AuthProvider.GOOGLE,
                        "google-sub",
                        "OAUTH@EXAMPLE.COM",
                        true,
                        "OAuth User",
                        "https://example.com/profile.png"
                );
            }
        };
    }

    @Test
    void oauthLoginDoesNotLinkExistingUserWhenProviderEmailIsUnverified() {
        AuthProperties authProperties = properties();
        JwtTokenService jwtTokenService = new JwtTokenService(authProperties, JsonMapper.builder().build());
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOAuthAccountRepository accountRepository = mock(UserOAuthAccountRepository.class);
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        OAuthProviderClient providerClient = fakeUnverifiedGoogleClient();
        AuthService authService = new AuthService(
                authProperties,
                jwtTokenService,
                currentUserProvider,
                userRepository,
                accountRepository,
                refreshTokenRepository,
                transactionManager(),
                List.of(providerClient)
        );

        when(accountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 11L);
            return user;
        });
        when(accountRepository.findByUserIdAndProvider(11L, AuthProvider.GOOGLE)).thenReturn(Optional.empty());
        when(accountRepository.saveAndFlush(any(UserOAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(AuthRefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokenResponse response = authService.oauthLogin(
                new OAuthLoginRequest(AuthProvider.GOOGLE, "id-token", null, "android")
        );

        assertThat(response.user().userId()).isEqualTo(11L);
        assertThat(response.user().email()).isNull();
        verify(userRepository, never()).findByEmail("oauth@example.com");
    }

    private OAuthProviderClient fakeUnverifiedGoogleClient() {
        return new OAuthProviderClient() {
            @Override
            public AuthProvider provider() {
                return AuthProvider.GOOGLE;
            }

            @Override
            public OAuthUserProfile verify(OAuthLoginRequest request) {
                return new OAuthUserProfile(
                        AuthProvider.GOOGLE,
                        "google-sub",
                        "OAUTH@EXAMPLE.COM",
                        false,
                        "OAuth User",
                        "https://example.com/profile.png"
                );
            }
        };
    }

    @Test
    void oauthProviderVerificationRunsOutsideTransaction() {
        AuthProperties authProperties = properties();
        JwtTokenService jwtTokenService = new JwtTokenService(authProperties, JsonMapper.builder().build());
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOAuthAccountRepository accountRepository = mock(UserOAuthAccountRepository.class);
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        TrackingTransactionManager transactionManager = new TrackingTransactionManager();
        OAuthProviderClient providerClient = providerClientAssertingNoTransaction(transactionManager);
        AuthService authService = new AuthService(
                authProperties,
                jwtTokenService,
                currentUserProvider,
                userRepository,
                accountRepository,
                refreshTokenRepository,
                transactionManager,
                List.of(providerClient)
        );

        when(accountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
                .thenAnswer(invocation -> {
                    assertThat(transactionManager.isActive()).isTrue();
                    return Optional.empty();
                });
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            assertThat(transactionManager.isActive()).isTrue();
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 12L);
            return user;
        });
        when(accountRepository.findByUserIdAndProvider(12L, AuthProvider.GOOGLE)).thenReturn(Optional.empty());
        when(accountRepository.saveAndFlush(any(UserOAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(AuthRefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokenResponse response = authService.oauthLogin(
                new OAuthLoginRequest(AuthProvider.GOOGLE, "id-token", null, "android")
        );

        assertThat(response.user().userId()).isEqualTo(12L);
    }

    @Test
    void oauthLoginRetriesProviderAccountRace() {
        AuthProperties authProperties = properties();
        JwtTokenService jwtTokenService = new JwtTokenService(authProperties, JsonMapper.builder().build());
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOAuthAccountRepository accountRepository = mock(UserOAuthAccountRepository.class);
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        OAuthProviderClient providerClient = fakeGoogleClient();
        AuthService authService = new AuthService(
                authProperties,
                jwtTokenService,
                currentUserProvider,
                userRepository,
                accountRepository,
                refreshTokenRepository,
                transactionManager(),
                List.of(providerClient)
        );

        User existingUser = user(20L, "oauth@example.com");
        UserOAuthAccount existingAccount = UserOAuthAccount.builder()
                .userId(20L)
                .provider(AuthProvider.GOOGLE)
                .providerUserId("google-sub")
                .email("oauth@example.com")
                .nickname("OAuth User")
                .profileImageUrl("https://example.com/profile.png")
                .build();
        when(accountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty(), Optional.of(existingAccount));
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 20L);
            return user;
        });
        when(accountRepository.findByUserIdAndProvider(20L, AuthProvider.GOOGLE)).thenReturn(Optional.empty());
        when(accountRepository.saveAndFlush(any(UserOAuthAccount.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate provider account"));
        when(userRepository.findById(20L)).thenReturn(Optional.of(existingUser));
        when(refreshTokenRepository.save(any(AuthRefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokenResponse response = authService.oauthLogin(
                new OAuthLoginRequest(AuthProvider.GOOGLE, "id-token", null, "android")
        );

        assertThat(response.user().userId()).isEqualTo(20L);
        assertThat(response.user().email()).isEqualTo("oauth@example.com");
    }

    @Test
    void oauthLoginRejectsVerifiedEmailUpdateWhenEmailBelongsToAnotherUser() {
        AuthProperties authProperties = properties();
        JwtTokenService jwtTokenService = new JwtTokenService(authProperties, JsonMapper.builder().build());
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOAuthAccountRepository accountRepository = mock(UserOAuthAccountRepository.class);
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        OAuthProviderClient providerClient = fakeGoogleClient();
        AuthService authService = new AuthService(
                authProperties,
                jwtTokenService,
                currentUserProvider,
                userRepository,
                accountRepository,
                refreshTokenRepository,
                transactionManager(),
                List.of(providerClient)
        );

        User currentUser = user(30L, null);
        User otherUser = user(31L, "oauth@example.com");
        UserOAuthAccount existingAccount = UserOAuthAccount.builder()
                .userId(30L)
                .provider(AuthProvider.GOOGLE)
                .providerUserId("google-sub")
                .email(null)
                .nickname("OAuth User")
                .profileImageUrl("https://example.com/profile.png")
                .build();
        when(accountRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.of(existingAccount));
        when(userRepository.findById(30L)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.of(otherUser));

        Throwable thrown = catchThrowable(() -> authService.oauthLogin(
                new OAuthLoginRequest(AuthProvider.GOOGLE, "id-token", null, "android")
        ));

        assertThat(thrown).isInstanceOfSatisfying(AuthException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.OAUTH_ACCOUNT_CONFLICT));
    }

    @Test
    void refreshWhenRevokedTokenIsReusedBurnsActiveTokenChainAndRejects() {
        AuthProperties authProperties = properties();
        JwtTokenService jwtTokenService = new JwtTokenService(authProperties, JsonMapper.builder().build());
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOAuthAccountRepository accountRepository = mock(UserOAuthAccountRepository.class);
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        AuthService authService = new AuthService(
                authProperties,
                jwtTokenService,
                currentUserProvider,
                userRepository,
                accountRepository,
                refreshTokenRepository,
                transactionManager(),
                List.of()
        );
        String refreshTokenValue = "rotated-refresh-token";
        String tokenHash = jwtTokenService.hashRefreshToken(refreshTokenValue);
        AuthRefreshToken reusedToken = AuthRefreshToken.builder()
                .userId(40L)
                .tokenHash(tokenHash)
                .deviceId("android")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        reusedToken.revoke();
        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(reusedToken));
        when(refreshTokenRepository.revokeActiveByUserIdAndDeviceId(
                eq(40L), eq("android"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);

        Throwable thrown = catchThrowable(() -> authService.refresh(
                new TokenRefreshRequest(refreshTokenValue, "android")
        ));

        assertThat(thrown).isInstanceOfSatisfying(AuthException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
        verify(refreshTokenRepository).revokeActiveByUserIdAndDeviceId(
                eq(40L), eq("android"), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(refreshTokenRepository, never()).save(any(AuthRefreshToken.class));
    }

    @Test
    void refreshWhenExpiredRevokedTokenIsReusedStillBurnsActiveTokenChain() {
        AuthProperties authProperties = properties();
        JwtTokenService jwtTokenService = new JwtTokenService(authProperties, JsonMapper.builder().build());
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOAuthAccountRepository accountRepository = mock(UserOAuthAccountRepository.class);
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        AuthService authService = new AuthService(
                authProperties,
                jwtTokenService,
                currentUserProvider,
                userRepository,
                accountRepository,
                refreshTokenRepository,
                transactionManager(),
                List.of()
        );
        String refreshTokenValue = "expired-rotated-refresh-token";
        String tokenHash = jwtTokenService.hashRefreshToken(refreshTokenValue);
        AuthRefreshToken reusedToken = AuthRefreshToken.builder()
                .userId(40L)
                .tokenHash(tokenHash)
                .deviceId("android")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        reusedToken.revoke();
        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(reusedToken));
        when(refreshTokenRepository.revokeActiveByUserIdAndDeviceId(
                eq(40L), eq("android"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1);

        Throwable thrown = catchThrowable(() -> authService.refresh(
                new TokenRefreshRequest(refreshTokenValue, "android")
        ));

        assertThat(thrown).isInstanceOfSatisfying(AuthException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));
        verify(refreshTokenRepository).revokeActiveByUserIdAndDeviceId(
                eq(40L), eq("android"), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(refreshTokenRepository, never()).save(any(AuthRefreshToken.class));
    }

    @Test
    void refreshKeepsOriginalDeviceIdEvenWhenRequestSendsDifferentDeviceId() {
        AuthProperties authProperties = properties();
        JwtTokenService jwtTokenService = new JwtTokenService(authProperties, JsonMapper.builder().build());
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOAuthAccountRepository accountRepository = mock(UserOAuthAccountRepository.class);
        AuthRefreshTokenRepository refreshTokenRepository = mock(AuthRefreshTokenRepository.class);
        AuthService authService = new AuthService(
                authProperties,
                jwtTokenService,
                currentUserProvider,
                userRepository,
                accountRepository,
                refreshTokenRepository,
                transactionManager(),
                List.of()
        );
        String refreshTokenValue = "active-refresh-token";
        String tokenHash = jwtTokenService.hashRefreshToken(refreshTokenValue);
        AuthRefreshToken activeToken = AuthRefreshToken.builder()
                .userId(41L)
                .tokenHash(tokenHash)
                .deviceId("original-device")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(activeToken, "id", 410L);
        User user = user(41L, "refresh@example.com");
        when(refreshTokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(activeToken));
        when(userRepository.findById(41L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(AuthRefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<AuthRefreshToken> savedToken = ArgumentCaptor.forClass(AuthRefreshToken.class);

        AuthTokenResponse response = authService.refresh(
                new TokenRefreshRequest(refreshTokenValue, "changed-device")
        );

        assertThat(response.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(savedToken.capture());
        assertThat(savedToken.getValue().getDeviceId()).isEqualTo("original-device");
        assertThat(savedToken.getValue().getRotatedFromId()).isEqualTo(410L);
    }

    private OAuthProviderClient providerClientAssertingNoTransaction(TrackingTransactionManager transactionManager) {
        return new OAuthProviderClient() {
            @Override
            public AuthProvider provider() {
                return AuthProvider.GOOGLE;
            }

            @Override
            public OAuthUserProfile verify(OAuthLoginRequest request) {
                assertThat(transactionManager.isActive()).isFalse();
                return new OAuthUserProfile(
                        AuthProvider.GOOGLE,
                        "google-sub",
                        "OAUTH@EXAMPLE.COM",
                        true,
                        "OAuth User",
                        "https://example.com/profile.png"
                );
            }
        };
    }

    private PlatformTransactionManager transactionManager() {
        return new TrackingTransactionManager();
    }

    private User user(Long id, String email) {
        User user = User.builder()
                .email(email)
                .nickname("OAuth User")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static class TrackingTransactionManager extends AbstractPlatformTransactionManager {
        private final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);

        boolean isActive() {
            return active.get();
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            active.set(true);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }

        @Override
        protected void doCleanupAfterCompletion(Object transaction) {
            active.remove();
        }
    }
}
