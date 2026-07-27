package com.cadence.codingassessmentservice.entity;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.constants.AssessmentType;
import com.cadence.codingassessmentservice.constants.Difficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

/** The recruiter's assessment definition -- matches the 4-step creation wizard field-for-field. allowedLanguages is a comma-separated column, not a child table (simple multi-select chip list, same simplification precedent as job-service's own flat string fields). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "assessment")
public class Assessment extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "created_by_recruiter_id")
    private UUID createdByRecruiterId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AssessmentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssessmentStatus status = AssessmentStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "passing_score_percent", nullable = false)
    private int passingScorePercent;

    @Column(name = "total_marks", nullable = false)
    private int totalMarks;

    @Column(name = "compiler_version", length = 50)
    private String compilerVersion;

    @Column(name = "allowed_languages", nullable = false, length = 200)
    private String allowedLanguages;

    @Column(name = "negative_marking", nullable = false)
    @Builder.Default
    private boolean negativeMarking = false;

    @Column(name = "anti_cheat_monitoring", nullable = false)
    @Builder.Default
    private boolean antiCheatMonitoring = true;

    @Column(name = "plagiarism_detection", nullable = false)
    @Builder.Default
    private boolean plagiarismDetection = true;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "candidate_instructions", columnDefinition = "TEXT")
    private String candidateInstructions;
}
