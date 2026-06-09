package com.startup.domain.auth.repository;

import com.startup.domain.auth.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);
}
