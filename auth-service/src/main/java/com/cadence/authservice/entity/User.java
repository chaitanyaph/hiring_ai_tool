package com.cadence.authservice.entity;

import com.cadence.authservice.constant.AuthProvider;
import com.cadence.authservice.constant.UserStatus;
import com.cadence.authservice.constant.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Core identity record. Deliberately kept lean (auth concerns only) —
 * profile data (resume, company details, etc.) lives in the owning
 * services (Candidate Service, Company Service) and is linked only by
 * this user's id, respecting the "each microservice owns its own data"
 * database-per-service strategy.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction("is_deleted = false")
public class User implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 30)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Builder.Default
    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 64)
    private String lastLoginIp;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 120)
    private String providerId;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

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

    public boolean isAccountNonLocked() {
        return accountLockedUntil == null || accountLockedUntil.isBefore(LocalDateTime.now());
    }
}
