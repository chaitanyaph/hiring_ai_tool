package com.cadence.aiinterviewservice.entity;

import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row per application_id (unique) -- a re-analysis (rare, e.g. resume re-matched) updates this row in place, same precedent as ParsedResume/ResumeMatch. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "candidate_shortlist")
public class CandidateShortlist extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "resume_match_id")
    private UUID resumeMatchId;

    @Column(name = "overall_match_score")
    private Integer overallMatchScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private ShortlistDecision decision;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "assigned_recruiter_id")
    private UUID assignedRecruiterId;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}
