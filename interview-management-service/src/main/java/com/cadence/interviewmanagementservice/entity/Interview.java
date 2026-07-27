package com.cadence.interviewmanagementservice.entity;

import com.cadence.interviewmanagementservice.constants.InterviewMode;
import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.constants.RoundType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Aggregate root -- one row per scheduled attempt. Absorbs the
 * suggested interview_schedule and meeting_details tables (reschedule
 * updates this row in place; interview_activity_log carries history),
 * and interview_status (a plain enum column, no lookup table).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "interview")
public class Interview extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "interview_round_id")
    private UUID interviewRoundId;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 20)
    private RoundType roundType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private int durationMinutes = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    @Builder.Default
    private InterviewMode mode = InterviewMode.ONLINE;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "auto_generate_meet_link", nullable = false)
    @Builder.Default
    private boolean autoGenerateMeetLink = true;

    @Column(name = "notify_candidate_by_email", nullable = false)
    @Builder.Default
    private boolean notifyCandidateByEmail = true;

    @Column(name = "notes_for_panel", columnDefinition = "TEXT")
    private String notesForPanel;

    @Column(name = "created_by_recruiter_id")
    private UUID createdByRecruiterId;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "reschedule_reason", length = 500)
    private String rescheduleReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
