package com.startup.domain.auth.repository;

import com.startup.domain.auth.entity.AuthRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from AuthRefreshToken token where token.tokenHash = :tokenHash")
    Optional<AuthRefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
