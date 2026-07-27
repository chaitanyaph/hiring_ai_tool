package com.cadence.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mfa_secrets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaSecret {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "secret_key", nullable = false)
    private String secretKey; // stored encrypted at rest via AttributeConverter in production KMS setup

    @Builder.Default
    @Column(nullable = false)
    private boolean confirmed = false;

    @Column(name = "recovery_codes", columnDefinition = "TEXT")
    private String recoveryCodes; // comma separated hashed codes

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
