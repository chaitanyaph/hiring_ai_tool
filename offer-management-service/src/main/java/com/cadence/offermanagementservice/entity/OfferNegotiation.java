package com.cadence.offermanagementservice.entity;

import com.cadence.offermanagementservice.constants.NegotiationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "offer_negotiation")
public class OfferNegotiation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "proposed_ctc", precision = 12, scale = 2)
    private BigDecimal proposedCtc;

    @Column(name = "message", length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NegotiationStatus status = NegotiationStatus.PENDING;

    @Column(name = "recruiter_notes", length = 500)
    private String recruiterNotes;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
