package com.cadence.aiinterviewservice.entity;

import com.cadence.aiinterviewservice.constants.InterviewMode;
import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row per application_id (unique) -- created in NOT_STARTED once shortlisted, advanced by both the recruiter's "Start" action and the candidate's own start. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "interview_session")
public class InterviewSession extends BaseAuditEntity {

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
    @Column(name = "mode", length = 10)
    private InterviewMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InterviewSessionStatus status = InterviewSessionStatus.NOT_STARTED;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private int totalQuestions = 8;

    @Column(name = "current_question_index", nullable = false)
    @Builder.Default
    private int currentQuestionIndex = 0;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
}
