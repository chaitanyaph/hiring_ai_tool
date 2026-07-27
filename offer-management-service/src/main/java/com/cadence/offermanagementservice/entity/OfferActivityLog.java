package com.cadence.offermanagementservice.entity;

import com.cadence.offermanagementservice.constants.ActivityEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Append-only -- backs both the recruiter drawer's "Approval timeline" and the /history list endpoint. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "offer_activity_log")
public class OfferActivityLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ActivityEventType eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column(name = "details", length = 1000)
    private String details;
}
