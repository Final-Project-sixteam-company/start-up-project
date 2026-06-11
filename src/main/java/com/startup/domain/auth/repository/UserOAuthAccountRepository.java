package com.startup.domain.auth.repository;

import com.startup.domain.auth.entity.UserOAuthAccount;
import com.startup.domain.auth.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, Long> {

    Optional<UserOAuthAccount> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    Optional<UserOAuthAccount> findByUserIdAndProvider(Long userId, AuthProvider provider);
}
