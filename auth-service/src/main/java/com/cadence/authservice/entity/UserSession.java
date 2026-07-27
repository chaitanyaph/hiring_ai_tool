package com.cadence.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "refresh_token_id")
    private UUID refreshTokenId;

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(length = 120)
    private String location;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastActiveAt = now;
    }
}
