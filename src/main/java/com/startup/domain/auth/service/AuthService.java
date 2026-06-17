package com.startup.domain.auth.service;

import com.startup.common.auth.AuthenticatedUserPrincipal;
import com.startup.common.auth.CurrentUserProvider;
import com.startup.domain.auth.dto.AuthMeResponse;
import com.startup.domain.auth.dto.AuthTokenResponse;
import com.startup.domain.auth.dto.DevLoginRequest;
import com.startup.domain.auth.dto.LogoutRequest;
import com.startup.domain.auth.dto.OAuthLoginRequest;
import com.startup.domain.auth.dto.TokenRefreshRequest;
import com.startup.domain.auth.dto.TossLoginRequest;
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
import com.startup.domain.auth.support.TossOAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthProperties authProperties;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final UserOAuthAccountRepository userOAuthAccountRepository;
    private final AuthRefreshTokenRepository authRefreshTokenRepository;
    private final PlatformTransactionManager transactionManager;
    private final List<OAuthProviderClient> oAuthProviderClients;
    private final TossOAuthClient tossOAuthClient;

    @Transactional
    public AuthTokenResponse devLogin(DevLoginRequest request) {
        if (!authProperties.isDevLoginEnabled()) {
            throw new AuthException(AuthErrorCode.DEV_LOGIN_DISABLED);
        }

        String email = normalizeEmail(request.email());
        String nickname = normalizeBlank(request.nickname());
        User user = userOAuthAccountRepository.findByProviderAndProviderUserId(AuthProvider.DEV, email)
                .flatMap(account -> userRepository.findById(account.getUserId()))
                .orElseGet(() -> findOrCreateDevUser(email, nickname));

        ensureUserActive(user);
        user.updateProfile(email, nickname, user.getProfileImageUrl());

        userOAuthAccountRepository.findByUserIdAndProvider(user.getId(), AuthProvider.DEV)
                .ifPresentOrElse(
                        account -> account.updateProfile(email, nickname, user.getProfileImageUrl()),
                        () -> userOAuthAccountRepository.save(UserOAuthAccount.builder()
                                .userId(user.getId())
                                .provider(AuthProvider.DEV)
                                .providerUserId(email)
                                .email(email)
                                .nickname(nickname)
                                .profileImageUrl(user.getProfileImageUrl())
                                .build())
                );

        return issueTokenPair(user, request.deviceId(), null);
    }

    public AuthTokenResponse oauthLogin(OAuthLoginRequest request) {
        if (request.provider() == AuthProvider.DEV) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
        }

        OAuthProviderClient providerClient = providerClientMap().get(request.provider());
        if (providerClient == null) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_SUPPORTED);
        }

        OAuthUserProfile profile = providerClient.verify(request);
        try {
            return loginOAuthUserInTransaction(profile, request.deviceId());
        } catch (DataIntegrityViolationException e) {
            log.warn("OAuth login raced with another request. Retrying by provider account lookup. provider={}",
                    profile.provider());
            return retryOAuthLoginAfterRace(profile, request.deviceId());
        }
    }

    public AuthTokenResponse tossLogin(TossLoginRequest request) {
        OAuthUserProfile profile = tossOAuthClient.verify(request);
        try {
            return loginOAuthUserInTransaction(profile, request.deviceId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Toss login raced with another request. Retrying by provider account lookup. provider={}",
                    profile.provider());
            return retryOAuthLoginAfterRace(profile, request.deviceId());
        }
    }

    private AuthTokenResponse loginOAuthUserInTransaction(OAuthUserProfile profile, String deviceId) {
        return Objects.requireNonNull(new TransactionTemplate(transactionManager).execute(status ->
                loginVerifiedOAuthUser(profile, deviceId)));
    }

    private AuthTokenResponse retryOAuthLoginAfterRace(OAuthUserProfile profile, String deviceId) {
        try {
            return loginOAuthUserInTransaction(profile, deviceId);
        } catch (DataIntegrityViolationException retryFailure) {
            throw new AuthException(AuthErrorCode.OAUTH_ACCOUNT_CONFLICT);
        }
    }

    private AuthTokenResponse loginVerifiedOAuthUser(OAuthUserProfile profile, String deviceId) {
        User user = loginOrCreateOAuthUser(profile);
        ensureUserActive(user);
        return issueTokenPair(user, deviceId, null);
    }

    @Transactional
    public AuthTokenResponse refresh(TokenRefreshRequest request) {
        String tokenHash = jwtTokenService.hashRefreshToken(request.refreshToken());
        AuthRefreshToken refreshToken = authRefreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (refreshToken.getRevokedAt() != null) {
            burnRefreshTokenChain(refreshToken, now);
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (refreshToken.isExpired(now)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        ensureUserActive(user);

        refreshToken.revoke();
        return issueTokenPair(user, refreshToken.getDeviceId(), refreshToken.getId());
    }

    private void burnRefreshTokenChain(AuthRefreshToken reusedToken, LocalDateTime now) {
        if (reusedToken.getRevokedAt() == null) {
            return;
        }

        LocalDateTime revokedAt = LocalDateTime.now();
        String deviceId = normalizeBlank(reusedToken.getDeviceId());
        int revokedCount = deviceId == null
                ? authRefreshTokenRepository.revokeActiveByUserIdAndNullDeviceId(
                        reusedToken.getUserId(), now, revokedAt)
                : authRefreshTokenRepository.revokeActiveByUserIdAndDeviceId(
                        reusedToken.getUserId(), deviceId, now, revokedAt);

        log.warn("Refresh token reuse detected. Active refresh token chain revoked. userId={}, deviceIdPresent={}, revokedCount={}",
                reusedToken.getUserId(), deviceId != null, revokedCount);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = jwtTokenService.hashRefreshToken(request.refreshToken());
        authRefreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(AuthRefreshToken::revoke);
    }

    @Transactional(readOnly = true)
    public AuthMeResponse me() {
        AuthenticatedUserPrincipal principal = currentUserProvider.requireAuthenticatedPrincipal();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        ensureUserActive(user);
        return new AuthMeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole()
        );
    }

    private User findOrCreateDevUser(String email, String nickname) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .nickname(nickname)
                        .build()));
    }

    private User loginOrCreateOAuthUser(OAuthUserProfile profile) {
        return userOAuthAccountRepository.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
                .map(account -> updateExistingOAuthUser(account, profile))
                .orElseGet(() -> createOrLinkOAuthUser(profile));
    }

    private User updateExistingOAuthUser(UserOAuthAccount account, OAuthUserProfile profile) {
        User user = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
        String verifiedEmail = verifiedEmailOrNull(profile);
        ensureVerifiedEmailAvailableForUser(user, verifiedEmail);
        user.updateProfile(verifiedEmail == null ? user.getEmail() : verifiedEmail,
                profile.nickname(), profile.profileImageUrl());
        account.updateProfile(verifiedEmail == null ? account.getEmail() : verifiedEmail,
                profile.nickname(), profile.profileImageUrl());
        return user;
    }

    private User createOrLinkOAuthUser(OAuthUserProfile profile) {
        String verifiedEmail = verifiedEmailOrNull(profile);
        User user = findUserByEmail(verifiedEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(verifiedEmail)
                        .nickname(profile.nickname())
                        .profileImageUrl(profile.profileImageUrl())
                        .build()));

        userOAuthAccountRepository.findByUserIdAndProvider(user.getId(), profile.provider())
                .ifPresent(existing -> {
                    throw new AuthException(AuthErrorCode.OAUTH_ACCOUNT_CONFLICT);
                });

        userOAuthAccountRepository.saveAndFlush(UserOAuthAccount.builder()
                .userId(user.getId())
                .provider(profile.provider())
                .providerUserId(profile.providerUserId())
                .email(verifiedEmail)
                .nickname(profile.nickname())
                .profileImageUrl(profile.profileImageUrl())
                .build());
        user.updateProfile(verifiedEmail == null ? user.getEmail() : verifiedEmail,
                profile.nickname(), profile.profileImageUrl());
        return user;
    }

    private Optional<User> findUserByEmail(String email) {
        String normalizedEmail = normalizeEmailOrNull(email);
        if (normalizedEmail == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(normalizedEmail);
    }

    private void ensureVerifiedEmailAvailableForUser(User user, String verifiedEmail) {
        String normalizedEmail = normalizeEmailOrNull(verifiedEmail);
        String currentEmail = normalizeEmailOrNull(user.getEmail());
        if (normalizedEmail == null || Objects.equals(normalizedEmail, currentEmail)) {
            return;
        }
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !Objects.equals(existing.getId(), user.getId()))
                .ifPresent(existing -> {
                    throw new AuthException(AuthErrorCode.OAUTH_ACCOUNT_CONFLICT);
                });
    }

    private AuthTokenResponse issueTokenPair(User user, String deviceId, Long rotatedFromId) {
        String accessToken = jwtTokenService.issueAccessToken(user);
        String refreshToken = jwtTokenService.generateRefreshToken();
        authRefreshTokenRepository.save(AuthRefreshToken.builder()
                .userId(user.getId())
                .tokenHash(jwtTokenService.hashRefreshToken(refreshToken))
                .deviceId(deviceId)
                .expiresAt(jwtTokenService.refreshTokenExpiresAt())
                .rotatedFromId(rotatedFromId)
                .build());

        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenService.accessTokenExpiresInSeconds(),
                toSummary(user)
        );
    }

    private AuthTokenResponse.UserSummary toSummary(User user) {
        return new AuthTokenResponse.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole()
        );
    }

    private void ensureUserActive(User user) {
        if (!user.isActive()) {
            throw new AuthException(AuthErrorCode.USER_NOT_ACTIVE);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmailOrNull(String email) {
        String normalized = normalizeBlank(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String verifiedEmailOrNull(OAuthUserProfile profile) {
        if (!profile.emailVerified()) {
            return null;
        }
        return normalizeEmailOrNull(profile.email());
    }

    private Map<AuthProvider, OAuthProviderClient> providerClientMap() {
        return oAuthProviderClients.stream()
                .collect(Collectors.toMap(OAuthProviderClient::provider, Function.identity()));
    }
}
