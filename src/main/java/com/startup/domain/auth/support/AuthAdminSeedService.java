package com.startup.domain.auth.support;

import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.enums.UserRole;
import com.startup.domain.auth.enums.UserStatus;
import com.startup.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAdminSeedService {

    private static final int MAX_EMAIL_LENGTH = 255;
    private static final String DEFAULT_ADMIN_NICKNAME = "ClueRoom Admin";

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    public void seedIfEnabled() {
        AuthProperties.AdminSeed seed = authProperties.getAdminSeed();
        if (!seed.isEnabled()) {
            return;
        }

        String email = normalizeEmail(seed.getEmail());
        String nickname = normalizeNickname(seed.getNickname());
        ensureAdminWithRetry(email, nickname);
    }

    private void ensureAdminWithRetry(String email, String nickname) {
        try {
            ensureAdminInNewTransaction(email, nickname);
        } catch (DataIntegrityViolationException e) {
            log.warn("Auth admin seed insert raced with another instance. Retrying by lookup.");
            ensureAdminInNewTransaction(email, nickname);
        }
    }

    private void ensureAdminInNewTransaction(String email, String nickname) {
        Objects.requireNonNull(new TransactionTemplate(transactionManager).execute(status -> {
            ensureAdmin(email, nickname);
            return Boolean.TRUE;
        }));
    }

    private void ensureAdmin(String email, String nickname) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.saveAndFlush(User.builder()
                        .email(email)
                        .nickname(nickname)
                        .role(UserRole.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build()));

        if (!user.isActive()) {
            throw new IllegalStateException("AUTH_ADMIN_SEED_EMAIL points to a non-active user.");
        }

        user.promoteToAdmin();
        log.info("Auth admin seed ensured: email={}", maskEmail(email));
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("AUTH_ADMIN_SEED_EMAIL is required when AUTH_ADMIN_SEED_ENABLED=true.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!isValidEmail(normalized)) {
            throw new IllegalStateException("AUTH_ADMIN_SEED_EMAIL is invalid.");
        }
        return normalized;
    }

    private boolean isValidEmail(String email) {
        int at = email.indexOf('@');
        int lastAt = email.lastIndexOf('@');
        return email.length() <= MAX_EMAIL_LENGTH
                && at > 0
                && at == lastAt
                && at < email.length() - 1
                && email.indexOf(' ', 0) < 0;
    }

    private String normalizeNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return DEFAULT_ADMIN_NICKNAME;
        }
        return nickname.trim();
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
