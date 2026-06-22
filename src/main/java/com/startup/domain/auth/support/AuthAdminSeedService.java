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
    private static final String DEFAULT_QA_NICKNAME = "ClueRoom QA";

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    public void seedIfEnabled() {
        AuthProperties.AdminSeed adminSeed = authProperties.getAdminSeed();
        AuthProperties.QaSeed qaSeed = authProperties.getQaSeed();
        String adminEmail = null;
        String qaEmail = null;

        if (adminSeed.isEnabled()) {
            adminEmail = normalizeEmail(adminSeed.getEmail(), "AUTH_ADMIN_SEED_EMAIL");
        }
        if (qaSeed.isEnabled()) {
            qaEmail = normalizeEmail(qaSeed.getEmail(), "AUTH_QA_SEED_EMAIL");
        }
        if (adminEmail != null && adminEmail.equals(qaEmail)) {
            throw new IllegalStateException("AUTH_QA_SEED_EMAIL must differ from AUTH_ADMIN_SEED_EMAIL.");
        }

        if (adminEmail != null) {
            ensureAdminWithRetry(adminEmail, normalizeNickname(adminSeed.getNickname(), DEFAULT_ADMIN_NICKNAME));
        }
        if (qaEmail != null) {
            ensureQaWithRetry(qaEmail, normalizeNickname(qaSeed.getNickname(), DEFAULT_QA_NICKNAME));
        }
    }

    private void ensureAdminWithRetry(String email, String nickname) {
        try {
            ensureAdminInNewTransaction(email, nickname);
        } catch (DataIntegrityViolationException e) {
            log.warn("Auth admin seed insert raced with another instance. Retrying by lookup.");
            ensureAdminInNewTransaction(email, nickname);
        }
    }

    private void ensureQaWithRetry(String email, String nickname) {
        try {
            ensureQaInNewTransaction(email, nickname);
        } catch (DataIntegrityViolationException e) {
            log.warn("Auth QA seed insert raced with another instance. Retrying by lookup.");
            ensureQaInNewTransaction(email, nickname);
        }
    }

    private void ensureAdminInNewTransaction(String email, String nickname) {
        Objects.requireNonNull(new TransactionTemplate(transactionManager).execute(status -> {
            ensureAdmin(email, nickname);
            return Boolean.TRUE;
        }));
    }

    private void ensureQaInNewTransaction(String email, String nickname) {
        Objects.requireNonNull(new TransactionTemplate(transactionManager).execute(status -> {
            ensureQaUser(email, nickname);
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

    private void ensureQaUser(String email, String nickname) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.saveAndFlush(User.builder()
                        .email(email)
                        .nickname(nickname)
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build()));

        if (!user.isActive()) {
            throw new IllegalStateException("AUTH_QA_SEED_EMAIL points to a non-active user.");
        }

        log.info("Auth QA seed ensured: email={}", maskEmail(email));
    }

    private String normalizeEmail(String email, String envName) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException(envName + " is required when " + enabledEnvName(envName) + "=true.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!isValidEmail(normalized)) {
            throw new IllegalStateException(envName + " is invalid.");
        }
        return normalized;
    }

    private String enabledEnvName(String emailEnvName) {
        return emailEnvName.replace("_EMAIL", "_ENABLED");
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

    private String normalizeNickname(String nickname, String defaultNickname) {
        if (!StringUtils.hasText(nickname)) {
            return defaultNickname;
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
