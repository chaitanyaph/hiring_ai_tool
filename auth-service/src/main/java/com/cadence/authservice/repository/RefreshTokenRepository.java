package com.cadence.authservice.repository;

import com.cadence.authservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true, rt.revokedAt = :now where rt.userId = :userId and rt.revoked = false")
    void revokeAllForUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < :threshold")
    void deleteAllExpiredBefore(@Param("threshold") LocalDateTime threshold);
}
