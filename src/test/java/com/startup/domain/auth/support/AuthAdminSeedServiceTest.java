package com.startup.domain.auth.support;

import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.enums.UserRole;
import com.startup.domain.auth.enums.UserStatus;
import com.startup.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;

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
    private final AuthAdminSeedService authAdminSeedService = new AuthAdminSeedService(authProperties, userRepository);

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
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authAdminSeedService.seedIfEnabled();

        verify(userRepository).save(any(User.class));
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                "admin@example.com".equals(user.getEmail())
                        && "Admin Tester".equals(user.getNickname())
                        && user.getRole() == UserRole.ADMIN
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
        verify(userRepository, never()).save(any(User.class));
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
}
