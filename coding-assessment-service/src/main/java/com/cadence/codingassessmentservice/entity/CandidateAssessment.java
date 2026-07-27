package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.constants.ProgrammingLanguage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row per (assessment_id, application_id) -- doubles as the invitation record and the live attempt/progress tracker, same dual role as ai-interview-service's interview_session. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "candidate_assessment")
public class CandidateAssessment extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CandidateAssessmentStatus status = CandidateAssessmentStatus.NOT_STARTED;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "reminded_at")
    private LocalDateTime remindedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "rules_accepted_at")
    private LocalDateTime rulesAcceptedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "current_question_index", nullable = false)
    @Builder.Default
    private int currentQuestionIndex = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_preference", length = 20)
    private ProgrammingLanguage languagePreference;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "test_cases_passed")
    private Integer testCasesPassed;

    @Column(name = "test_cases_total")
    private Integer testCasesTotal;

    @Column(name = "time_used_seconds")
    private Integer timeUsedSeconds;

    @Column(name = "auto_submitted", nullable = false)
    @Builder.Default
    private boolean autoSubmitted = false;
}
