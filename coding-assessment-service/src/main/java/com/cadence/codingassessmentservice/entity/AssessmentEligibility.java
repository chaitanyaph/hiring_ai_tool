package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.HiringRecommendation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row per application_id (unique) -- populated by consuming CandidateRecommended, since Application Service doesn't yet consume that event itself (a documented gap on ai-interview-service's side) and this service needs to know who's eligible to invite. */
@Entity
@Table(name = "assessment_eligibility")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentEligibility {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "hiring_recommendation", nullable = false, length = 20)
    private HiringRecommendation hiringRecommendation;

    @Column(name = "recommended_at", nullable = false)
    private LocalDateTime recommendedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
