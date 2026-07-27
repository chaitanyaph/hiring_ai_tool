package com.cadence.resumeparserservice.entity;

import com.cadence.resumeparserservice.constants.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row per application_id (unique) -- recalculate updates this row
 * in place (incrementing attemptCount), same precedent as
 * ParsedResume. resumeId/parsedResumeId are nullable because a row can
 * exist in AWAITING_RESUME/AWAITING_PARSE before either is known.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "resume_match")
public class ResumeMatch extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "application_id", nullable = false, unique = true)
    private UUID applicationId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    // Denormalized from Job Service at analysis time -- lets the
    // Recommendations feed filter by department without an N+1 Feign
    // call per row.
    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "resume_id")
    private UUID resumeId;

    @Column(name = "parsed_resume_id")
    private UUID parsedResumeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ResumeMatchStatus status = ResumeMatchStatus.AWAITING_RESUME;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_used", length = 20)
    private AiProvider providerUsed;

    @Column(name = "overall_match_score")
    private Integer overallMatchScore;

    @Column(name = "technical_skill_score")
    private Integer technicalSkillScore;

    @Column(name = "programming_language_score")
    private Integer programmingLanguageScore;

    @Column(name = "framework_score")
    private Integer frameworkScore;

    @Column(name = "database_score")
    private Integer databaseScore;

    @Column(name = "cloud_score")
    private Integer cloudScore;

    @Column(name = "devops_score")
    private Integer devopsScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_match_label", length = 20)
    private ExperienceMatchLabel experienceMatchLabel;

    @Column(name = "project_match_score")
    private Integer projectMatchScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_match_label", length = 20)
    private EducationMatchLabel educationMatchLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "certification_match_label", length = 20)
    private CertificationMatchLabel certificationMatchLabel;

    @Column(name = "communication_prediction")
    private Integer communicationPrediction;

    @Column(name = "problem_solving_prediction")
    private Integer problemSolvingPrediction;

    @Column(name = "learning_ability_prediction")
    private Integer learningAbilityPrediction;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
}
