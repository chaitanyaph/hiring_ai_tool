package com.cadence.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * We never store the raw refresh token — only a SHA-256 hash — so a
 * database leak alone cannot be used to mint new access tokens.
 * Rotation is modeled explicitly via replacedBy to detect reuse of a
 * revoked token (a strong signal of token theft).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Builder.Default
    @Column(name = "remember_me", nullable = false)
    private boolean rememberMe = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
