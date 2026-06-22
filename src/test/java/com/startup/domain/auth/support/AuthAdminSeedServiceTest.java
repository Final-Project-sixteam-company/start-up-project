package com.startup.domain.auth.support;

import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.enums.UserRole;
import com.startup.domain.auth.enums.UserStatus;
import com.startup.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthAdminSeedServiceTest {

    private final AuthProperties authProperties = new AuthProperties();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthAdminSeedService authAdminSeedService = new AuthAdminSeedService(
            authProperties,
            userRepository,
            new NoOpTransactionManager()
    );

    @Test
    void seedIfEnabledDoesNothingWhenDisabled() {
        authProperties.getAdminSeed().setEnabled(false);

        authAdminSeedService.seedIfEnabled();

        verifyNoInteractions(userRepository);
    }

    @Test
    void seedIfEnabledCreatesAdminUserWithNormalizedEmail() {
        authProperties.getAdminSeed().setEnabled(true);
        authProperties.getAdminSeed().setEmail(" ADMIN@EXAMPLE.COM ");
        authProperties.getAdminSeed().setNickname(" Admin Tester ");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authAdminSeedService.seedIfEnabled();

        verify(userRepository).saveAndFlush(any(User.class));
        verify(userRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(user ->
                "admin@example.com".equals(user.getEmail())
                        && "Admin Tester".equals(user.getNickname())
                        && user.getRole() == UserRole.ADMIN
                        && user.getStatus() == UserStatus.ACTIVE
        ));
    }

    @Test
    void seedIfEnabledCreatesQaUserWithNormalizedEmail() {
        authProperties.getQaSeed().setEnabled(true);
        authProperties.getQaSeed().setEmail(" QA@EXAMPLE.COM ");
        authProperties.getQaSeed().setNickname(" QA Tester ");
        when(userRepository.findByEmail("qa@example.com")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authAdminSeedService.seedIfEnabled();

        verify(userRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(user ->
                "qa@example.com".equals(user.getEmail())
                        && "QA Tester".equals(user.getNickname())
                        && user.getRole() == UserRole.USER
                        && user.getStatus() == UserStatus.ACTIVE
        ));
    }

    @Test
    void seedIfEnabledPromotesExistingActiveUser() {
        authProperties.getAdminSeed().setEnabled(true);
        authProperties.getAdminSeed().setEmail("admin@example.com");
        User user = User.builder()
                .email("admin@example.com")
                .nickname("Admin Tester")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        authAdminSeedService.seedIfEnabled();

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void seedIfEnabledReusesExistingActiveQaUserWithoutChangingRole() {
        authProperties.getQaSeed().setEnabled(true);
        authProperties.getQaSeed().setEmail("qa@example.com");
        User user = User.builder()
                .email("qa@example.com")
                .nickname("QA Tester")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("qa@example.com")).thenReturn(Optional.of(user));

        authAdminSeedService.seedIfEnabled();

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void seedIfEnabledRejectsBlankEmail() {
        authProperties.getAdminSeed().setEnabled(true);
        authProperties.getAdminSeed().setEmail(" ");

        assertThatThrownBy(authAdminSeedService::seedIfEnabled)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_ADMIN_SEED_EMAIL");
    }

    @Test
    void seedIfEnabledRejectsBlankQaEmail() {
        authProperties.getQaSeed().setEnabled(true);
        authProperties.getQaSeed().setEmail(" ");

        assertThatThrownBy(authAdminSeedService::seedIfEnabled)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_QA_SEED_EMAIL");
    }

    @Test
    void seedIfEnabledRejectsDuplicateAdminAndQaEmail() {
        authProperties.getAdminSeed().setEnabled(true);
        authProperties.getAdminSeed().setEmail("staff@example.com");
        authProperties.getQaSeed().setEnabled(true);
        authProperties.getQaSeed().setEmail(" STAFF@EXAMPLE.COM ");

        assertThatThrownBy(authAdminSeedService::seedIfEnabled)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_QA_SEED_EMAIL");
        verifyNoInteractions(userRepository);
    }

    @Test
    void seedIfEnabledRejectsInactiveExistingUser() {
        authProperties.getAdminSeed().setEnabled(true);
        authProperties.getAdminSeed().setEmail("admin@example.com");
        User user = User.builder()
                .email("admin@example.com")
                .nickname("Admin Tester")
                .role(UserRole.USER)
                .status(UserStatus.BLOCKED)
                .build();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(authAdminSeedService::seedIfEnabled)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-active");
    }

    @Test
    void seedIfEnabledRejectsInactiveExistingQaUser() {
        authProperties.getQaSeed().setEnabled(true);
        authProperties.getQaSeed().setEmail("qa@example.com");
        User user = User.builder()
                .email("qa@example.com")
                .nickname("QA Tester")
                .role(UserRole.USER)
                .status(UserStatus.WITHDRAWN)
                .build();
        when(userRepository.findByEmail("qa@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(authAdminSeedService::seedIfEnabled)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-active");
    }

    @Test
    void seedIfEnabledRetriesWhenConcurrentInsertWins() {
        authProperties.getAdminSeed().setEnabled(true);
        authProperties.getAdminSeed().setEmail("admin@example.com");
        User user = User.builder()
                .email("admin@example.com")
                .nickname("Admin Tester")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        authAdminSeedService.seedIfEnabled();

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void seedIfEnabledRetriesQaSeedWhenConcurrentInsertWins() {
        authProperties.getQaSeed().setEnabled(true);
        authProperties.getQaSeed().setEmail("qa@example.com");
        User user = User.builder()
                .email("qa@example.com")
                .nickname("QA Tester")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("qa@example.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        authAdminSeedService.seedIfEnabled();

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).saveAndFlush(any(User.class));
    }

    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
