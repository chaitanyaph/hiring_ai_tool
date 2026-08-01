package com.cadence.companyservice.entity;

import com.cadence.companyservice.constant.InvitationStatus;
import com.cadence.companyservice.constant.TeamRole;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Invitation lifecycle only -- Company Service never creates a login
 * account. Auth Service does that once the invite link is accepted, and
 * publishes InvitationAccepted so this row can be marked ACCEPTED.
 *
 * No soft-delete here: CANCELLED is already a terminal status in the
 * lifecycle, so there's no separate "deleted" state to model.
 */
@Entity
@Table(name = "team_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamInvitation implements Serializable {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TeamRole role;

    @Column(name = "invite_token", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private String inviteToken;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

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

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }
}
